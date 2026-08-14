// Umbrales mínimos exigidos por el Bloque C.5 / A.1.
// Perfil móvil: npx lhci autorun (contra contenedor recién levantado,
// throttling Slow 4G).
// Perfil escritorio (Bloque A.1, exige ambos perfiles): lhci no admite
// dos configuraciones de collect en el mismo archivo, así que se corre
// aparte con la CLI de Lighthouse y el preset oficial de escritorio,
// que fija su propio throttling apropiado (no reusar la config de
// arriba con form-factor=desktop: ver la nota metodológica en
// docs/mediciones/lighthouse/REPORT.md sobre por qué eso da un
// resultado invalido):
//   npx lighthouse https://localhost:8443 --preset=desktop \
//     --output=json,html --output-path=docs/mediciones/lighthouse/desktop-runN
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
