describe('Vehicle List Page (/vehicles)', () => {
  const mockVehiclesPage0 = {
    content: [
      {
        id: 1,
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2024,
        categorie: 'SUV',
        ville: 'Casablanca',
        prixJour: 350,
        photoUrl: 'https://example.com/duster.jpg',
      },
      {
        id: 2,
        marque: 'Renault',
        modele: 'Clio 5',
        annee: 2023,
        categorie: 'Citadine',
        ville: 'Marrakech',
        prixJour: 280,
        photoUrl: 'https://example.com/clio.jpg',
      },
      {
        id: 3,
        marque: 'Peugeot',
        modele: '208',
        annee: 2024,
        categorie: 'Citadine',
        ville: 'Rabat',
        prixJour: 300,
        photoUrl: null,
      },
    ],
    page: {
      size: 12,
      number: 0,
      totalElements: 15,
      totalPages: 2,
    },
  };

  const mockVehiclesPage1 = {
    content: [
      {
        id: 13,
        marque: 'Volkswagen',
        modele: 'Golf 8',
        annee: 2024,
        categorie: 'Berline',
        ville: 'Tanger',
        prixJour: 450,
        photoUrl: null,
      },
    ],
    page: {
      size: 12,
      number: 1,
      totalElements: 15,
      totalPages: 2,
    },
  };

  it('displays the vehicle grid with cards, lazy-loaded photos, and details on /vehicles', () => {
    cy.intercept('GET', '/api/vehicles*', (req) => {
      if (req.query.page === '1') {
        req.reply({ statusCode: 200, body: mockVehiclesPage1 });
      } else {
        req.reply({ statusCode: 200, body: mockVehiclesPage0 });
      }
    }).as('getVehicles');

    cy.visit('/vehicles');
    cy.wait('@getVehicles');

    cy.get('[data-cy="vehicles-grid"]').should('be.visible');
    cy.get('[data-cy="vehicle-card"]').should('have.length', 3);

    cy.get('[data-cy="vehicle-card"]')
      .first()
      .within(() => {
        cy.get('[data-cy="vehicle-photo"]').should('have.attr', 'loading', 'lazy');
        cy.get('[data-cy="vehicle-brand"]').should('contain.text', 'Dacia');
        cy.get('[data-cy="vehicle-model"]').should('contain.text', 'Duster');
        cy.get('[data-cy="vehicle-year"]').should('contain.text', '2024');
        cy.get('[data-cy="vehicle-city"]').should('contain.text', 'Casablanca');
        cy.get('[data-cy="vehicle-category"]').should('contain.text', 'SUV');
        cy.get('[data-cy="vehicle-price"]').should('contain.text', '350');
        cy.get('[data-cy="details-btn"]').should('be.visible');
      });
  });

  it('toggles the favorite (heart) button on a vehicle card', () => {
    cy.intercept('GET', '/api/vehicles*', {
      statusCode: 200,
      body: mockVehiclesPage0,
    }).as('getVehicles');

    cy.visit('/vehicles');
    cy.wait('@getVehicles');

    cy.get('[data-cy="favorite-btn"]')
      .first()
      .should('have.attr', 'aria-pressed', 'false')
      .click()
      .should('have.attr', 'aria-pressed', 'true')
      .and('have.class', 'active');
  });

  it('changes page using MatPaginator', () => {
    cy.intercept('GET', '/api/vehicles*', (req) => {
      if (req.query.page === '1') {
        req.reply({ statusCode: 200, body: mockVehiclesPage1 });
      } else {
        req.reply({ statusCode: 200, body: mockVehiclesPage0 });
      }
    }).as('getVehicles');

    cy.visit('/vehicles');
    cy.wait('@getVehicles');

    cy.get('mat-paginator button.mat-mdc-paginator-navigation-next').click();
    cy.wait('@getVehicles');

    cy.get('[data-cy="vehicle-card"]').should('have.length', 1);
    cy.get('[data-cy="vehicle-brand"]').first().should('contain.text', 'Volkswagen');
  });

  it('displays an empty state message when no vehicles match', () => {
    cy.intercept('GET', '/api/vehicles*', {
      statusCode: 200,
      body: {
        content: [],
        page: { size: 12, number: 0, totalElements: 0, totalPages: 0 },
      },
    }).as('getEmptyVehicles');

    cy.visit('/vehicles');
    cy.wait('@getEmptyVehicles');

    cy.get('[data-cy="empty-state"]').should('be.visible');
    cy.get('[data-cy="vehicle-card"]').should('have.length', 0);
  });

  it('adapts responsive grid across mobile, tablet, and desktop viewports', () => {
    cy.intercept('GET', '/api/vehicles*', {
      statusCode: 200,
      body: mockVehiclesPage0,
    }).as('getVehicles');

    // Mobile (1 colonne)
    cy.viewport(375, 667);
    cy.visit('/vehicles');
    cy.wait('@getVehicles');
    cy.get('[data-cy="vehicles-grid"]').should('be.visible');

    // Tablette (2 colonnes)
    cy.viewport(768, 1024);
    cy.get('[data-cy="vehicles-grid"]').should('be.visible');

    // Desktop (3-4 colonnes)
    cy.viewport(1280, 800);
    cy.get('[data-cy="vehicles-grid"]').should('be.visible');
  });
});
