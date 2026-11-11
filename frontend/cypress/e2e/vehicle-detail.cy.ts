describe('Vehicle Detail Page (/vehicles/:id) - Navigation & galerie', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: data.categories });

      cy.intercept('GET', '/api/vehicles/1/availability*', {
        statusCode: 200,
        body: data.availability,
      }).as('getAvailability');

      cy.intercept('GET', '/api/vehicles/1', {
        statusCode: 200,
        body: data.vehicleDetail,
      }).as('getVehicle');

      cy.intercept('GET', '/api/vehicles*', (req) => {
        if (req.query['categorie'] === 'SUV') {
          req.reply({ statusCode: 200, body: data.similarVehicles });
        } else {
          req.reply({ statusCode: 200, body: data.page0 });
        }
      }).as('getVehiclesList');
    });
  });

  it('navigates from catalogue list (/vehicles) to vehicle detail page (/vehicles/1)', () => {
    cy.visit('/vehicles');
    cy.wait('@getVehiclesList');

    cy.get('[data-cy="vehicle-card"]').first().find('[data-cy="details-btn"]').click();
    cy.url().should('include', '/vehicles/1');
    cy.wait('@getVehicle');
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');
  });

  it('displays full vehicle specifications, pricing, city and agency on /vehicles/1', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');

    // Specifications
    cy.get('[data-cy="vehicle-brand"]').should('contain.text', 'Dacia');
    cy.get('[data-cy="vehicle-model"]').should('contain.text', 'Duster');
    cy.get('[data-cy="vehicle-year"]').should('contain.text', '2024');
    cy.get('[data-cy="vehicle-category"]').should('contain.text', 'SUV');
    cy.get('[data-cy="vehicle-fuel"]').should('contain.text', 'DIESEL');
    cy.get('[data-cy="vehicle-transmission"]').should('contain.text', 'MANUELLE');
    cy.get('[data-cy="vehicle-seats"]').should('contain.text', '5');
    cy.get('[data-cy="vehicle-description"]').should('contain.text', 'SUV compact');
    cy.get('[data-cy="vehicle-matricule"]').should('contain.text', '78945-A-1');

    // Pricing
    cy.get('[data-cy="vehicle-rent-price"]').should('contain.text', '350');
    cy.get('[data-cy="vehicle-sale-price"]').should('contain.text', '195000');

    // City and Agency
    cy.get('[data-cy="vehicle-city"]').should('contain.text', 'Casablanca');
    cy.get('[data-cy="vehicle-agency"]').should('contain.text', 'LocaVite Casablanca Marina');
  });

  it('navigates through photo gallery with buttons and thumbnails', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="photo-gallery"]').should('be.visible');
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '1 / 3');

    // Next button
    cy.get('[data-cy="gallery-next"]').click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '2 / 3');

    // Next button again
    cy.get('[data-cy="gallery-next"]').click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '3 / 3');

    // Thumbnail click to go back to 1st photo
    cy.get('[data-cy="gallery-thumbnail"]').first().click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '1 / 3');
  });

  it('displays the monthly availability calendar and highlights dates', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="vehicle-tabs"]').contains('Disponibilité').click();
    cy.get('[data-cy="availability-calendar"]').should('be.visible');
    cy.get('[data-cy="calendar-legend"]').should('be.visible');
    cy.get('[data-cy="mat-calendar"]').should('be.visible');
  });

  it('navigates to /reservation/:id when clicking Réserver', () => {
    cy.login('client@locavite.ma', 'ClientPass123!', ['CLIENT']);
    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 2,
        nom: 'Client',
        prenom: 'Test',
        email: 'client@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    });
    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [
        { id: 1, type: 'PERMIS', status: 'VALIDATED', url: '/permis.pdf', uploadedAt: '2026-01-16T10:00:00' },
        { id: 2, type: 'CIN', status: 'VALIDATED', url: '/cin.pdf', uploadedAt: '2026-01-16T11:00:00' },
      ],
    });

    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="reserve-btn"]').should('be.visible').click();
    cy.url().should('include', '/reservation/1');
    cy.get('[data-cy="reservation-page"]').should('be.visible');
  });

  it('opens contact modal with WhatsApp and contact form on clicking Contacter agence', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="contact-agency-btn"]').click({ force: true });
    cy.get('[data-cy="contact-dialog"]').should('be.visible');

    // WhatsApp action button
    cy.get('[data-cy="whatsapp-contact-btn"]').should('be.visible');

    // Contact form submit
    cy.get('[data-cy="contact-name-input"]').type('Yassine Alami', { force: true });
    cy.get('[data-cy="contact-email-input"]').type('yassine@example.com', { force: true });
    cy.get('[data-cy="contact-phone-input"]').type('0612345678', { force: true });

    cy.get('[data-cy="send-contact-btn"]').click({ force: true });
    cy.get('[data-cy="contact-success-alert"]').should('be.visible');

    cy.get('[data-cy="contact-dialog"] button').contains(/Fermer|Close/).click({ force: true });
    cy.get('[data-cy="contact-dialog"]').should('not.exist');
  });

  it('toggles favorite button on vehicle detail page', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="favorite-btn"]')
      .first()
      .should('have.attr', 'aria-pressed', 'false')
      .click()
      .should('have.attr', 'aria-pressed', 'true')
      .and('have.class', 'active');
  });

  it('displays 3 similar vehicle suggestions excluding current vehicle', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="similar-vehicles"]').should('be.visible');
    cy.get('[data-cy="similar-vehicles"] [data-cy="vehicle-card"]').should('have.length', 3);

    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Renault');
    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Peugeot');
    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Hyundai');
  });

  it('sets dynamic SEO title and meta tags', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.title().should('contain', 'Dacia Duster (2024)');
    cy.get('meta[name="description"]')
      .should('have.attr', 'content')
      .and('include', 'SUV compact');

    cy.get('meta[property="og:title"]')
      .should('have.attr', 'content')
      .and('include', 'Dacia Duster');
  });

  it('adapts responsive layout across mobile, tablet, and desktop', () => {
    cy.viewport(375, 667);
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');
    cy.get('[data-cy="reserve-btn"]').should('be.visible');

    cy.viewport(768, 1024);
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');

    cy.viewport(1280, 800);
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');
    cy.get('[data-cy="action-card"]').should('be.visible');
  });
});
