// Benchmark del endpoint de listado protegido — ESCENARIO CACHÉ FRÍA (Bloque C.1).
// Mismo perfil de carga que el escenario caliente para comparación válida:
// 50 VUs, 30 s, ramp-up declarado. La diferencia es la clave de caché:
// cada petición usa una página distinta del rango con datos (seed sintético:
// 2.401 estudiantes activos => 241 páginas de tamaño 10), por lo que el valor
// solicitado NO está aún en Redis y cada GET es un cache miss que cae a
// PostgreSQL con filtros, paginado y JOINs reales.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 50 },   // ramp-up declarado
    { duration: '30s', target: 50 },  // meseta de medición
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    // Umbral objetivo del Bloque C.1 para caché fría: p95 < 500 ms
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate==0'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ username: 'admin', password: 'sged2026' }),
    { headers: { 'Content-Type': 'application/json' } });
  const raw = res.headers['Set-Cookie'] || res.headers['set-cookie'] || '';
  const match = raw.match(/sged_access=([^;]+)/);
  return { access: match ? match[1] : '' };
}

export default function (data) {
  // Página única por (VU, iteración) dentro del rango con datos (0..240):
  // ninguna clave de caché se repite dentro de la corrida => cache miss real.
  const p = (__VU - 1 + __ITER) % 241;
  const res = http.get(`${BASE}/api/estudiantes?page=${p}&size=10`, {
    cookies: { sged_access: data.access },
  });
  check(res, {
    'status 200': (r) => r.status === 200,
    'tiene contenido': (r) => r.json('content') !== undefined,
  });
  sleep(0.1);
}