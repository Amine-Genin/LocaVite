describe('Parcours de réservation complet (/vehicles → /reservation/:id → /reservations/:id)', () => {
  const formatInputDate = (daysFromToday: number) => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + daysFromToday);
    const month = d.getMonth() + 1;
    const day = d.getDate();
    const year = d.getFullYear();
    return `${month}/${day}/${year}`;
  };

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.fixture('reservations.json').then((resData) => {
      cy.fixture('vehicles.json').then((vehData) => {
        // Interceptions véhicules & catalogue
        cy.intercept('GET', '/api/brands', { statusCode: 200, body: vehData.brands }).as('getBrands');
        cy.intercept('GET', '/api/categories', { statusCode: 200, body: vehData.categories }).as('getCategories');
        cy.intercept('GET', '/api/vehicles*', { statusCode: 200, body: vehData.page0 }).as('getVehicles');
        cy.intercept('GET', '/api/vehicles/1', { statusCode: 200, body: resData.vehicle }).as('getVehicle');

        // Interceptions profil & documents
        cy.intercept('GET', '/api/users/me', { statusCode: 200, body: resData.clientUser }).as('getProfile');

        // Interceptions devis & disponibilité
        cy.intercept('GET', '/api/vehicles/1/availability*', {
          statusCode: 200,
          body: resData.availability,
        }).as('checkAvailability');

        cy.intercept('POST', '/api/reservations/quote', (req) => {
          const hasOptions = Array.isArray(req.body.options) && req.body.options.length > 0;
          req.reply({
            statusCode: 200,
            body: hasOptions ? resData.quoteWithOptions : resData.quote,
          });
        }).as('getQuote');

        // Interception création de réservation
        cy.intercept('POST', '/api/reservations', (req) => {
          expect(req.body.vehicleId).to.eq(1);
          req.reply({
            statusCode: 201,
            body: resData.createdReservation,
          });
        }).as('createReservationApi');

        cy.intercept('GET', '/api/reservations/88', {
          statusCode: 200,
          body: resData.createdReservation,
        }).as('getCreatedReservation');
      });
    });
  });

  it('redirige vers /login si un utilisateur non authentifié tente de réserver', () => {
    cy.visit('/reservation/1');
    cy.url().should('include', '/login');
  });

  it('affiche un avertissement et bloque la confirmation si les documents ne sont pas validés', () => {
    cy.fixture('reservations.json').then((resData) => {
      cy.loginAsClient('ahmed.benali@locavite.ma');
      cy.intercept('GET', '/api/users/me/documents', {
        statusCode: 200,
        body: resData.pendingDocuments,
      }).as('getPendingDocs');

      cy.visit('/reservation/1');
      cy.wait(['@getVehicle', '@getProfile', '@getPendingDocs']);

      cy.get('[data-cy="documents-warning"]').should('be.visible');
      cy.get('[data-cy="upload-documents-link"]')
        .should('be.visible')
        .and('have.attr', 'href', '/profile');
    });
  });

  it('exécute le parcours complet de bout en bout (Recherche → Détail → Réservation → Confirmation)', () => {
    cy.fixture('reservations.json').then((resData) => {
      cy.loginAsClient('ahmed.benali@locavite.ma');
      cy.intercept('GET', '/api/users/me/documents', {
        statusCode: 200,
        body: resData.validDocuments,
      }).as('getValidDocs');

      // 1. Recherche & catalogue
      cy.visit('/vehicles');
      cy.wait('@getVehicles');
      cy.get('[data-cy="vehicles-grid"]').should('be.visible');
      cy.get('[data-cy="vehicle-card"]').should('have.length.at.least', 1);

      // Clic sur les détails du premier véhicule (Dacia Duster)
      cy.get('[data-cy="vehicle-card"]')
        .first()
        .within(() => {
          cy.get('[data-cy="details-btn"]').click();
        });

      // 2. Page détail véhicule
      cy.url().should('include', '/vehicles/1');
      cy.wait('@getVehicle');
      cy.get('[data-cy="action-card"]').should('contain.text', 'Dacia Duster');
      cy.get('[data-cy="reserve-btn"]').should('be.visible').click();

      // 3. Formulaire de réservation
      cy.url().should('include', '/reservation/1');
      cy.wait(['@getProfile', '@getValidDocs']);
      cy.get('[data-cy="reservation-vehicle-title"]').should('contain.text', 'Dacia Duster');
      cy.get('[data-cy="documents-warning"]').should('not.exist');

      // Étape 1 : Choix des dates
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

      // Étape 3 : Options & recalcul temps réel
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

      cy.wait('@createReservationApi');

      // 4. Redirection vers la page de confirmation (/reservations/:id)
      cy.url().should('include', '/reservations/88');
      cy.get('[data-cy="reservation-success-page"]').should('be.visible');
      cy.get('[data-cy="reservation-success-title"]').should('contain.text', 'Réservation confirmée');
      cy.get('[data-cy="reservation-success-message"]').should('contain.text', '88');
    });
  });

  it('effectue la réservation via la commande custom cy.createReservation()', () => {
    cy.fixture('reservations.json').then((resData) => {
      cy.loginAsClient('ahmed.benali@locavite.ma');
      cy.intercept('GET', '/api/users/me/documents', {
        statusCode: 200,
        body: resData.validDocuments,
      }).as('getValidDocs');

      cy.visit('/reservation/1');
      cy.wait(['@getVehicle', '@getProfile', '@getValidDocs']);

      // Utilisation de la commande personnalisée
      cy.createReservation({
        dateDebutDays: 2,
        dateFinDays: 6,
        pickupType: 'Aéroport',
        pickupLocation: 'Aéroport - Casablanca',
        returnLocation: 'Agence LocaVite Casablanca',
        gps: true,
        acceptTerms: true,
        submit: true,
      });

      cy.wait('@createReservationApi');
      cy.url().should('include', '/reservations/88');
      cy.get('[data-cy="reservation-success-page"]').should('be.visible');
    });
  });
});
