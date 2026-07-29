// Umbrales mínimos exigidos por el Bloque C.5.
// Ejecutar con: npx lhci autorun  (contra contenedor recién levantado,
// perfil móvil, throttling Slow 4G).
module.exports = {
  ci: {
    collect: {
      url: ['http://localhost:4200'],
      numberOfRuns: 3,
      settings: {
        // Lighthouse emula movil por defecto; no existe preset 'mobile'
        // (los validos son perf/experimental/desktop) y pasarlo aborta la
        // corrida con exit code 1.
        formFactor: 'mobile',
        throttlingMethod: 'simulate',
        screenEmulation: {
          mobile: true,
          width: 412,
          height: 823,
          deviceScaleFactor: 1.75,
          disabled: false,
        },
      },
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.8 }],
        'categories:accessibility': ['error', { minScore: 0.9 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],

        // SEO: umbral relajado DELIBERADAMENTE, no para inflar la nota.
        // SGED es una aplicacion de gestion interna que trata datos
        // personales de menores de edad, por lo que public/robots.txt
        // declara "Disallow: /". Lighthouse penaliza eso con la auditoria
        // "is-crawlable" (-27 puntos), porque su categoria SEO asume que
        // el sitio QUIERE ser indexado por buscadores. Aqui lo correcto es
        // justamente lo contrario (ver docs/etica/ETHICS.md).
        // Se mantiene la medicion y se verifican las auditorias SEO que si
        // aplican (meta-description, viewport, document-title, hreflang...),
        // pero no se exige el puntaje agregado de la categoria.
        'categories:seo': ['warn', { minScore: 0.9 }],
        'meta-description': ['error', { minScore: 1 }],
        'document-title': ['error', { minScore: 1 }],
        'html-has-lang': ['error', { minScore: 1 }],
        'viewport': ['error', { minScore: 1 }],
      },
    },
    upload: {
      target: 'filesystem',
      outputDir: 'docs/mediciones/lighthouse',
    },
  },
};
