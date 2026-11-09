describe('Vehicle Filters (/vehicles) - Filtres & synchronisation URL', () => {
  interface FixtureVehicle {
    id: number;
    marque: string;
    modele: string;
    annee: number;
    categorie: string;
    ville: string;
    carburant?: string;
    transmission?: string;
    prixJour: number;
    photoUrl?: string | null;
  }

  const buildPage = (vehicles: FixtureVehicle[]) => ({
    content: vehicles,
    page: {
      size: 12,
      number: 0,
      totalElements: vehicles.length,
      totalPages: vehicles.length > 0 ? 1 : 0,
    },
  });

  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('filters the vehicle list by city and keeps the filter synchronized in the URL', () => {
    cy.fixture('vehicles.json').then((data) => {
      const allVehicles: FixtureVehicle[] = data.page0.content;

      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: data.categories });
      cy.intercept('GET', '/api/vehicles*', (req) => {
        if (req.query['ville'] === 'Casablanca') {
          req.reply({
            statusCode: 200,
            body: buildPage(allVehicles.filter((v) => v.ville === 'Casablanca')),
          });
        } else {
          req.reply({ statusCode: 200, body: buildPage(allVehicles) });
        }
      }).as('searchVehicles');

      cy.visit('/vehicles');
      cy.wait('@searchVehicles');
      cy.get('[data-cy="vehicle-card"]').should('have.length', 3);

      cy.get('[data-cy="filter-city"]').type('Casablanca', { force: true });
      cy.wait('@searchVehicles');

      cy.location('search').should('contain', 'ville=Casablanca');
      cy.get('[data-cy="vehicle-card"]').should('have.length', 1);
      cy.get('[data-cy="results-count"]').should('contain.text', '1');
    });
  });

  it('applies category, brand, and sort filters and resets all filters cleanly', () => {
    cy.fixture('vehicles.json').then((data) => {
      const allVehicles: FixtureVehicle[] = data.page0.content;

      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: data.categories });
      cy.intercept('GET', '/api/vehicles*', (req) => {
        let filtered = [...allVehicles];
        if (req.query['categorie']) {
          filtered = filtered.filter((v) => v.categorie === req.query['categorie']);
        }
        if (req.query['marque']) {
          filtered = filtered.filter((v) => v.marque === req.query['marque']);
        }
        req.reply({ statusCode: 200, body: buildPage(filtered) });
      }).as('searchVehicles');

      cy.visit('/vehicles');
      cy.wait('@searchVehicles');

      // Select SUV category
      cy.get('[data-cy="filter-category"]').click({ force: true });
      cy.get('mat-option').contains('SUV').click();
      cy.wait('@searchVehicles');

      cy.location('search').should('contain', 'categorie=SUV');
      cy.get('[data-cy="vehicle-card"]').should('have.length', 1);

      // Select sort option
      cy.get('[data-cy="filter-sort"]').click({ force: true });
      cy.get('mat-option').eq(1).click();
      cy.wait('@searchVehicles');
      cy.location('search').should('contain', 'sort=prixJour');

      // Reset filters
      cy.get('[data-cy="reset-filters-btn"]').click({ force: true });
      cy.wait('@searchVehicles');

      cy.location('search').should('eq', '');
      cy.get('[data-cy="vehicle-card"]').should('have.length', 3);
    });
  });

  it('hydrates filter controls from initial URL query parameters', () => {
    cy.fixture('vehicles.json').then((data) => {
      const allVehicles: FixtureVehicle[] = data.page0.content;

      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: data.categories });
      cy.intercept('GET', '/api/vehicles*', {
        statusCode: 200,
        body: buildPage(allVehicles),
      }).as('searchVehicles');

      cy.visit('/vehicles?ville=Marrakech&sort=prixJour,asc');
      cy.wait('@searchVehicles');

      cy.get('[data-cy="filter-city"]').should('have.value', 'Marrakech');
      cy.location('search').should('contain', 'ville=Marrakech');
      cy.location('search').should('contain', 'sort=prixJour');
    });
  });

  it('opens and closes the filter drawer on mobile viewport', () => {
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: data.categories });
      cy.intercept('GET', '/api/vehicles*', { statusCode: 200, body: data.emptyPage });

      cy.viewport(375, 812);
      cy.visit('/vehicles');

      cy.get('.mobile-filter-toggle').click();
      cy.get('.filters-panel.drawer-open').should('be.visible');
      cy.get('.drawer-backdrop').should('exist').click({ force: true });
      cy.get('.filters-panel').should('not.be.visible');
    });
  });
});
