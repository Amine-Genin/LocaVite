import { defineConfig } from 'cypress';
// @ts-expect-error db-reset is a CommonJS script
import { resetDatabase } from './cypress/scripts/db-reset.cjs';

export default defineConfig({
  reporter: 'cypress/support/mochawesome-reporter.cjs',
  reporterOptions: {
    reportDir: 'cypress/reports/mochawesome',
    overwrite: false,
    html: true,
    json: true,
  },
  e2e: {
    baseUrl: 'http://localhost:4200',
    supportFile: 'cypress/support/e2e.ts',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    video: false,
    setupNodeEvents(on, config) {
      on('task', {
        'db:reset'() {
          resetDatabase();
          return null;
        },
        'db:seed'() {
          resetDatabase();
          return null;
        },
      });
      return config;
    },
  },
});
