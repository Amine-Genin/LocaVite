describe('Formulaire de réservation (/reservation/:vehicleId)', () => {
  const formatInputDate = (daysFromToday) => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + daysFromToday);
    const month = d.getMonth() + 1;
    const day = d.getDate();
    const year = d.getFullYear();
    return `${month}/${day}/${year}`;
  };

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/vehicles/1', {
        statusCode: 200,
        body: data.vehicleDetail,
      }).as('getVehicle');
    });

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 2,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    }).as('getProfile');
  });

  it('redirige vers /login si le client n’est pas authentifié', () => {
    cy.visit('/reservation/1');
    cy.url().should('include', '/login');
  });

  it('affiche un message d’alerte et un lien vers /profile si les documents ne sont pas validés', () => {
    cy.login('ahmed.benali@locavite.ma', 'Motdepasse123!', ['CLIENT']);
    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [
        {
          id: 10,
          type: 'PERMIS',
          status: 'PENDING',
          url: 'http://localhost:9000/locavite-documents/permis.pdf',
          uploadedAt: '2026-01-16T10:00:00',
        },
      ],
    }).as('getPendingDocs');

    cy.visit('/reservation/1');
    cy.wait(['@getVehicle', '@getProfile', '@getPendingDocs']);

    cy.get('[data-cy="documents-warning"]').should('be.visible');
    cy.get('[data-cy="upload-documents-link"]')
      .should('be.visible')
      .and('have.attr', 'href', '/profile');
  });

  it('parcours complet de réservation (Dates -> Lieux -> Options -> Récapitulatif -> Confirmation)', () => {
    cy.login('ahmed.benali@locavite.ma', 'Motdepasse123!', ['CLIENT']);

    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [
        {
          id: 10,
          type: 'PERMIS',
          status: 'VALIDATED',
          url: 'http://localhost:9000/locavite-documents/permis.pdf',
          uploadedAt: '2026-01-16T10:00:00',
        },
        {
          id: 11,
          type: 'CIN',
          status: 'VALIDATED',
          url: 'http://localhost:9000/locavite-documents/cin.pdf',
          uploadedAt: '2026-01-16T11:00:00',
        },
      ],
    }).as('getValidDocs');

    cy.intercept('GET', '/api/vehicles/1/availability*', {
      statusCode: 200,
      body: { available: ['2026-11-15', '2026-11-19'], unavailable: [] },
    }).as('checkAvailability');

    cy.intercept('POST', '/api/reservations/quote', (req) => {
      const hasOptions = Array.isArray(req.body.options) && req.body.options.length > 0;
      req.reply({
        statusCode: 200,
        body: {
          sousTotal: 1400,
          options: hasOptions ? 280 : 0,
          remise: 0,
          caution: 3000,
          tva: hasOptions ? 336 : 280,
          total: hasOptions ? 5016 : 4680,
          jours: 4,
        },
      });
    }).as('getQuote');

    cy.intercept('POST', '/api/reservations', (req) => {
      expect(req.body.vehicleId).to.eq(1);
      expect(req.body.options).to.include('GPS');
      req.reply({
        statusCode: 201,
        body: {
          id: 88,
          code: 'LV-RES88',
          statut: 'PENDING',
          prixTotal: 5016,
        },
      });
    }).as('createReservation');

    cy.visit('/reservation/1');
    cy.wait(['@getVehicle', '@getProfile', '@getValidDocs']);

    cy.get('[data-cy="reservation-vehicle-title"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-cy="documents-warning"]').should('not.exist');

    // Étape 1 : Dates
    cy.get('[data-cy="date-debut-input"]').clear().type(formatInputDate(2)).blur();
    cy.get('[data-cy="date-fin-input"]').clear().type(formatInputDate(6)).blur();

    cy.wait(['@checkAvailability', '@getQuote']);
    cy.get('[data-cy="availability-success"]').should('be.visible');
    cy.get('[data-cy="realtime-price-total"]').should('contain.text', '4680.00 MAD');

    cy.get('[data-cy="step-dates-next"]').should('not.be.disabled').click();

    // Étape 2 : Lieux
    cy.get('[data-cy="pickup-type-Aéroport"] input[type="radio"]').check({ force: true });
    cy.get('[data-cy="pickup-location-input"]').should('have.value', 'Aéroport - Casablanca');
    cy.get('[data-cy="return-location-input"]')
      .clear({ force: true })
      .type('Agence LocaVite Casablanca', { force: true });

    cy.get('[data-cy="step-places-next"]').should('not.be.disabled').click();

    // Étape 3 : Options + mise à jour du prix en temps réel
    cy.get('[data-cy="option-gps"] input[type="checkbox"]').check({ force: true });
    cy.wait('@getQuote');
    cy.get('[data-cy="realtime-price-total"]').should('contain.text', '5016.00 MAD');

    cy.get('[data-cy="step-options-next"]').click();

    // Étape 4 : Récapitulatif
    cy.get('[data-cy="price-subtotal"]').should('contain.text', '1400.00 MAD');
    cy.get('[data-cy="price-options"]').should('contain.text', '280.00 MAD');
    cy.get('[data-cy="price-caution"]').should('contain.text', '3000.00 MAD');
    cy.get('[data-cy="price-tva"]').should('contain.text', '336.00 MAD');
    cy.get('[data-cy="price-total"]').should('contain.text', '5016.00 MAD');

    cy.get('[data-cy="step-summary-next"]').should('not.be.disabled').click();

    // Étape 5 : Confirmation
    cy.get('[data-cy="confirm-reservation-btn"]').should('be.disabled');
    cy.get('[data-cy="accept-terms-checkbox"] input[type="checkbox"]').check({ force: true });
    cy.get('[data-cy="confirm-reservation-btn"]').should('not.be.disabled').click();

    cy.wait('@createReservation');

    // Redirection vers /reservations/:id
    cy.url().should('include', '/reservations/88');
    cy.get('[data-cy="reservation-success-page"]').should('be.visible');
    cy.get('[data-cy="reservation-success-title"]').should('contain.text', 'Réservation confirmée');
    cy.get('[data-cy="reservation-success-message"]').should('contain.text', '88');
  });
});
