#!/usr/bin/env node
/**
 * Auditoría Lighthouse repetible contra la URL pública (Bloque C.5 / A.1).
 *
 * Resuelve las dos limitaciones de la medición previa (que solo medía
 * localhost:8443 sin sesión):
 *   1. URL pública: `requestedUrl` apunta al despliegue real en Render.
 *   2. Sesión autenticada: se hace login real contra `/api/auth/login`
 *      (proxy del propio frontend) y la cookie `sged_access`/`sged_refresh`
 *      se inyecta por CDP (`Network.setCookie`) en el Chrome que reutiliza
 *      Lighthouse (`disableStorageReset: true`), con lo que cada ruta bajo
 *      `authGuard` se mide con la sesión activa, no con el redirect a /login.
 *
 * Se usan dos perfiles (móvil 412×823 DPR 1.75 y escritorio 1350×940,
 * ambos con `throttlingMethod: "simulate"`), dos rutas y 3 corridas por
 * par perfil×ruta = 12 LHR completos, todos con `requestedUrl` pública.
 *
 * Configuración por variables de entorno:
 *   LH_USER, LH_PASS    (obligatorias; van como secrets del workflow)
 *   LH_BASE_URL         (default: https://sged-frontend-jofa.onrender.com)
 *   LH_ROUTES           (default: "/dashboard /inventario")
 *   LH_RUNS             (default: 3)
 *   LH_OUTPUT           (default: docs/mediciones/lighthouse)
 *   LH_CHROME_PATH      (opcional; binario de Chrome/Chromium a usar)
 */

import { launch } from 'chrome-launcher';
import puppeteer from 'puppeteer-core';
import lighthouse from 'lighthouse';
import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

const USER = process.env.LH_USER;
const PASS = process.env.LH_PASS;
if (!USER || !PASS) {
  console.error('[lighthouse-ci] Faltan las variables LH_USER y LH_PASS.');
  process.exit(1);
}

const BASE_URL = (process.env.LH_BASE_URL || 'https://sged-frontend-jofa.onrender.com').replace(/\/+$/, '');
const ROUTES = (process.env.LH_ROUTES || '/dashboard /inventario').trim().split(/\s+/);
const RUNS = Number(process.env.LH_RUNS || 3);
const OUTPUT_DIR = process.env.LH_OUTPUT || 'docs/mediciones/lighthouse';
const HOST = new URL(BASE_URL).host;
const COOKIE_DOMAIN = HOST;

const PERFILES = {
  mobile: {
    formFactor: 'mobile',
    screenEmulation: { mobile: true, width: 412, height: 823, deviceScaleFactor: 1.75, disabled: false },
    throttlingMethod: 'simulate',
  },
  desktop: {
    formFactor: 'desktop',
    screenEmulation: { mobile: false, width: 1350, height: 940, deviceScaleFactor: 1, disabled: false },
    throttlingMethod: 'simulate',
    throttling: { rttMs: 40, throughputKbps: 10240, cpuSlowdownMultiplier: 1 },
  },
};
const CATEGORIAS = ['performance', 'accessibility', 'best-practices', 'seo'];
const ETIQUETA = {
  performance: 'Rendimiento',
  accessibility: 'Accesibilidad',
  'best-practices': 'Buenas prácticas',
  seo: 'SEO',
};

async function login(baseUrl, username, password) {
  const res = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const headers = res.headers;
  const raw =
    typeof headers.getSetCookie === 'function'
      ? headers.getSetCookie()
      : headers.get('set-cookie')
        ? [headers.get('set-cookie')]
        : [];
  const extraer = (nombre) => {
    const linea = raw.find((c) => c.startsWith(`${nombre}=`));
    return linea ? linea.split(';')[0].slice(nombre.length + 1) : null;
  };
  const acceso = extraer('sged_access');
  const refresh = extraer('sged_refresh');
  if (!acceso) {
    throw new Error(`Login ${res.status}: no se obtuvo la cookie sged_access.`);
  }
  return { acceso, refresh };
}

async function inyectarCookie(browser, username) {
  const cookies = await login(BASE_URL, username, PASS);
  const page = await browser.newPage();
  try {
    const cdp = await page.createCDPSession();
    await cdp.send('Network.setCookie', {
      url: `${BASE_URL}/`,
      name: 'sged_access',
      value: cookies.acceso,
      path: '/api',
      httpOnly: true,
      secure: true,
      sameSite: 'Strict',
    });
    if (cookies.refresh) {
      await cdp.send('Network.setCookie', {
        url: `${BASE_URL}/`,
        name: 'sged_refresh',
        value: cookies.refresh,
        path: '/api',
        httpOnly: true,
        secure: true,
        sameSite: 'Strict',
      });
    }
  } finally {
    await page.close();
  }
  console.log(`[lighthouse-ci] Sesión iniciada como "${username}" en ${HOST}`);
}

function nombreRuta(route) {
  return route.replace(/^\/+/, '').replace(/[^a-z0-9]+/gi, '-');
}

async function main() {
  console.log(`[lighthouse-ci] URL pública: ${BASE_URL}`);
  console.log(`[lighthouse-ci] Rutas: ${ROUTES.join(', ')} | corridas por perfil×ruta: ${RUNS}`);

  await mkdir(OUTPUT_DIR, { recursive: true });

  const chrome = await launch({
    chromePath: process.env.LH_CHROME_PATH || undefined,
    headless: true,
    chromeFlags: [
      '--remote-allow-origins=*',
      ...(typeof process.getuid === 'function' && process.getuid() === 0 ? ['--no-sandbox'] : []),
    ],
  });

  const resumen = { baseUrl: BASE_URL, fecha: new Date().toISOString(), rutas: ROUTES, corridas: RUNS, porPerfil: {} };
  try {
    const browser = await puppeteer.connect({
      browserURL: `http://127.0.0.1:${chrome.port}`,
      defaultViewport: null,
    });

    for (const [perfil, perfilFlags] of Object.entries(PERFILES)) {
      await inyectarCookie(browser, USER);
      resumen.porPerfil[perfil] = {};
      for (const route of ROUTES) {
        const targetUrl = `${BASE_URL}${route}`;
        const resultadosRuta = [];
        for (let run = 1; run <= RUNS; run++) {
          const salida = join(OUTPUT_DIR, `public-${perfil}-${nombreRuta(route)}-run${run}.report.json`);
          const flags = {
            port: chrome.port,
            onlyCategories: CATEGORIAS,
            logLevel: 'info',
            disableStorageReset: true,
            maxWaitForLoad: 60000,
            ...perfilFlags,
          };
          process.stdout.write(`[lighthouse-ci] ${perfil} ${route} corrida ${run}/${RUNS} … `);
          const resultado = await lighthouse(targetUrl, flags, undefined, undefined);
          const lhr = resultado.lhr;
          if (lhr.runtimeError) {
            throw new Error(`runtimeError en ${targetUrl}: ${lhr.runtimeError.message}`);
          }
          await writeFile(salida, resultado.report + '\n');
          const puntajes = Object.fromEntries(
            CATEGORIAS.map((c) => [c, Math.round(((lhr.categories[c]?.score ?? 0) * 100) * 100) / 100]),
          );
          resultadosRuta.push(puntajes);
          console.log(
            `ok (requestedUrl=${lhr.requestedUrl} finalUrl=${lhr.finalUrl || '-'}, ` +
              Object.entries(puntajes)
                .map(([c, s]) => `${c}=${s}`)
                .join(' ') +
              ')',
          );
        }
        const medias = Object.fromEntries(
          CATEGORIAS.map((cat) => [
            cat,
            resultadosRuta.reduce((acc, r) => acc + (r[cat] ?? 0), 0) / resultadosRuta.length,
          ]),
        );
        resumen.porPerfil[perfil][route] = { corridas: resultadosRuta, media: medias };
      }
    }
    await browser.disconnect();
  } finally {
    await chrome.kill();
  }

  await writeFile(join(OUTPUT_DIR, 'public-summary.json'), JSON.stringify(resumen, null, 2) + '\n');

  console.log('\n# Resumen Lighthouse público (URL real en producción)');
  for (const [perfil, rutas] of Object.entries(resumen.porPerfil)) {
    console.log(`\n## Perfil ${perfil}`);
    for (const [route, datos] of Object.entries(rutas)) {
      console.log(`\n| Ruta ${route} | Run 1 | Run 2 | Run 3 | Media |`);
      console.log('|---|---|---|---|---|');
      for (const label of Object.keys(datos.media)) {
        const runs = datos.corridas.map((r) => r[label] ?? 0).join(' | ');
        console.log(`| ${ETIQUETA[label] ?? label} | ${runs} | **${datos.media[label].toFixed(2)}** |`);
      }
    }
  }
  console.log(`\n[ok] LHR guardados en ${OUTPUT_DIR}/public-*.report.json`);
}

main().catch((err) => {
  console.error(`[lighthouse-ci] ERROR: ${err && err.stack ? err.stack : err}`);
  process.exit(1);
});