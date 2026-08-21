// Benchmark del endpoint de listado protegido (Bloque C.1).
// Configuración fija para comparación entre corridas (Bloque B.2):
// 50 VUs, 30 s, ramp-up declarado. Login por cookie HttpOnly.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 50 },   // ramp-up declarado
    { duration: '30s', target: 50 },  // meseta de medición
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    // Umbral objetivo: p95 < 200 ms con cache caliente (ISO/IEC 25010)
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate==0'],     // cero errores >= 500
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ username: 'admin', password: 'sged2026' }),
    { headers: { 'Content-Type': 'application/json' } });
  // Extraer cookie del header Set-Cookie (Path=/api)
  const raw = res.headers['Set-Cookie'] || res.headers['set-cookie'] || '';
  const match = raw.match(/sged_access=([^;]+)/);
  return { access: match ? match[1] : '' };
}

export default function (data) {
  const res = http.get(`${BASE}/api/estudiantes?page=0&size=10`, {
    cookies: { sged_access: data.access },
  });
  check(res, {
    'status 200': (r) => r.status === 200,
    'tiene contenido': (r) => r.json('content') !== undefined,
  });
  sleep(0.1);
}
