describe('Vehicle filters (/vehicles)', () => {
  const page = (vehicles) => ({
    content: vehicles,
    page: { size: 12, number: 0, totalElements: vehicles.length, totalPages: 1 },
  });

  it('filters the vehicle list and keeps the filter in the URL', () => {
    cy.intercept('GET', '/api/brands', [{ id: 1, name: 'Dacia' }]);
    cy.intercept('GET', '/api/categories', [{ id: 2, name: 'SUV' }]);
    cy.intercept('GET', '/api/vehicles*', (req) => {
      if (req.query.ville === 'Casablanca') {
        req.reply({ statusCode: 200, body: page([
          { id: 1, marque: 'Dacia', modele: 'Duster', annee: 2024, categorie: 'SUV', ville: 'Casablanca', prixJour: 350 },
        ]) });
      } else {
        req.reply({ statusCode: 200, body: page([
          { id: 1, marque: 'Dacia', modele: 'Duster', annee: 2024, categorie: 'SUV', ville: 'Casablanca', prixJour: 350 },
          { id: 2, marque: 'Renault', modele: 'Clio', annee: 2023, categorie: 'Citadine', ville: 'Marrakech', prixJour: 280 },
        ]) });
      }
    }).as('searchVehicles');

    cy.visit('/vehicles');
    cy.wait('@searchVehicles');
    cy.get('[data-cy="vehicle-card"]').should('have.length', 2);

    cy.get('[data-cy="filter-city"]').type('Casablanca');
    cy.wait('@searchVehicles');

    cy.location('search').should('contain', 'ville=Casablanca');
    cy.get('[data-cy="vehicle-card"]').should('have.length', 1);
    cy.get('[data-cy="results-count"]').should('contain.text', '1');
  });

  it('opens the filter drawer on mobile', () => {
    cy.intercept('GET', '/api/brands', []);
    cy.intercept('GET', '/api/categories', []);
    cy.intercept('GET', '/api/vehicles*', { statusCode: 200, body: page([]) });
    cy.viewport(375, 812);
    cy.visit('/vehicles');

    cy.get('.mobile-filter-toggle').click();
    cy.get('.filters-panel.drawer-open').should('be.visible');
    cy.get('.drawer-backdrop').should('be.visible').click({ force: true });
    cy.get('.filters-panel').should('not.be.visible');
  });
});
