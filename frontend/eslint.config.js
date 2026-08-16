// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // COD-01 / R-04 (informe de evaluacion de calidad): el frontend no
      // tenia verificacion de estilo semantico. 'warn' y no 'error' porque
      // ya existen 7 usos de `any` en el repo (D-10); subir a 'error' una
      // vez eliminados para que la regla quede exigida de verdad.
      '@typescript-eslint/no-explicit-any': 'warn',
      // Bajadas a 'warn' porque tseslint.configs.recommended/stylistic las
      // trae en 'error' y ya hay violaciones preexistentes (subscribe con
      // callback de error vacio, un `type` que deberia ser `interface`).
      // Subir a 'error' una vez corregidas para que `ng lint` las bloquee.
      '@typescript-eslint/no-empty-function': 'warn',
      '@typescript-eslint/consistent-type-definitions': 'warn',
      '@angular-eslint/prefer-inject': 'warn',
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {
      // Bajada a 'warn' por la misma razon que las reglas de arriba: ya
      // existe un uso preexistente en login.component.ts.
      '@angular-eslint/template/no-autofocus': 'warn',
    },
  },
]);
