// Umbrales mínimos exigidos por el Bloque C.5.
// Ejecutar con: npx lhci autorun  (contra contenedor recién levantado,
// perfil móvil, throttling Slow 4G).
module.exports = {
  ci: {
    collect: {
      url: ['http://localhost:4200'],
      numberOfRuns: 3,
      settings: {
        preset: 'mobile',
        throttlingMethod: 'simulate',
      },
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.8 }],
        'categories:accessibility': ['error', { minScore: 0.9 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],
        'categories:seo': ['error', { minScore: 0.9 }],
      },
    },
    upload: {
      target: 'filesystem',
      outputDir: 'docs/mediciones/lighthouse',
    },
  },
};
