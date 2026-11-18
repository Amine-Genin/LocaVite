describe('Parcours de paiement en espèces (CASH_DELIVERY, CASH_AGENCY)', () => {
  const mockReservation = {
    id: 88,
    code: 'RES-8801',
    userId: 2,
    vehicleId: 1,
    vehicle: {
      id: 1,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      prixJour: 350,
      ville: 'Casablanca',
      statut: 'PUBLISHED',
    },
    dateDebut: '2026-12-01',
    dateFin: '2026-12-05',
    lieuPriseEnCharge: 'Aéroport Mohammed V - Casablanca',
    lieuRetour: 'Agence LocaVite Casablanca',
    prixTotal: 1400.0,
    statut: 'PENDING',
  };

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/reservations/88', {
      statusCode: 200,
      body: mockReservation,
    }).as('getReservation');
  });

  it('permet de choisir le paiement en espèces à la livraison (CASH_DELIVERY)', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('POST', '/api/payments/cash', (req) => {
      expect(req.body.reservationId).to.eq(88);
      expect(req.body.montant).to.eq(1400);
      expect(req.body.methode).to.eq('CASH_DELIVERY');
      req.reply({
        statusCode: 200,
        body: {
          id: 51,
          code: 'PAY-CSH051',
          reservationId: 88,
          montant: 1400.0,
          devise: 'MAD',
          methode: 'CASH_DELIVERY',
          statut: 'PENDING',
          createdAt: '2026-11-18T10:00:00Z',
        },
      });
    }).as('payCashDelivery');

    cy.intercept('GET', '/api/payments/51', {
      statusCode: 200,
      body: {
        id: 51,
        code: 'PAY-CSH051',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CASH_DELIVERY',
        statut: 'PENDING',
        createdAt: '2026-11-18T10:00:00Z',
      },
    }).as('getStatus51');

    cy.visit('/payment/88');
    cy.wait('@getReservation');

    // Sélection Espèces à la livraison
    cy.get('[data-cy="method-cash-delivery-card"]').click();
    cy.get('[data-cy="method-cash-delivery-card"]').should('have.class', 'selected');

    cy.get('[data-cy="submit-payment-btn"]').click();
    cy.wait('@payCashDelivery');

    cy.url().should('include', '/payment/success');
    cy.url().should('include', 'method=CASH_DELIVERY');
    cy.wait('@getStatus51');

    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="payment-code"]').should('contain.text', 'PAY-CSH051');
    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00051');
  });

  it('permet de choisir le paiement en espèces en agence (CASH_AGENCY)', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('POST', '/api/payments/cash', (req) => {
      expect(req.body.reservationId).to.eq(88);
      expect(req.body.montant).to.eq(1400);
      expect(req.body.methode).to.eq('CASH_AGENCY');
      req.reply({
        statusCode: 200,
        body: {
          id: 52,
          code: 'PAY-CSH052',
          reservationId: 88,
          montant: 1400.0,
          devise: 'MAD',
          methode: 'CASH_AGENCY',
          statut: 'PENDING',
          createdAt: '2026-11-18T10:00:00Z',
        },
      });
    }).as('payCashAgency');

    cy.intercept('GET', '/api/payments/52', {
      statusCode: 200,
      body: {
        id: 52,
        code: 'PAY-CSH052',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CASH_AGENCY',
        statut: 'PENDING',
        createdAt: '2026-11-18T10:00:00Z',
      },
    }).as('getStatus52');

    cy.visit('/payment/88');
    cy.wait('@getReservation');

    // Sélection Espèces en agence
    cy.get('[data-cy="method-cash-agency-card"]').click();
    cy.get('[data-cy="method-cash-agency-card"]').should('have.class', 'selected');

    cy.get('[data-cy="submit-payment-btn"]').click();
    cy.wait('@payCashAgency');

    cy.url().should('include', '/payment/success');
    cy.url().should('include', 'method=CASH_AGENCY');
    cy.wait('@getStatus52');

    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="payment-code"]').should('contain.text', 'PAY-CSH052');
  });

  it('permet à l’administrateur de confirmer la réception d’un paiement en espèces en attente', () => {
    const pendingCashPayment = {
      id: 52,
      code: 'PAY-CSH052',
      reservationId: 88,
      clientNom: 'Ahmed Benali',
      montant: 1400.0,
      devise: 'MAD',
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
      createdAt: '2026-11-18T10:00:00Z',
    };

    cy.intercept('GET', /\/api\/payments(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: [pendingCashPayment],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    }).as('getAdminPayments');

    cy.intercept('PUT', '/api/admin/payments/52/confirm-cash', {
      statusCode: 200,
      body: {
        ...pendingCashPayment,
        statut: 'SUCCESS',
        paidAt: '2026-11-18T10:30:00Z',
      },
    }).as('confirmCashPayment');

    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getAdminPayments');

    cy.get('[data-testid="payments-table"]').should('be.visible');
    cy.contains('PAY-CSH052').should('be.visible');
    cy.contains('CASH_AGENCY').should('be.visible');

    // Confirmation de la boîte de dialogue native
    cy.on('window:confirm', () => true);

    // Clic sur le bouton de confirmation espèces
    cy.get('[data-testid="btn-confirm-cash"]').should('be.visible').click();
    cy.wait('@confirmCashPayment');

    // Message de succès dans la snackbar
    cy.contains('PAY-CSH052').should('exist');
  });
});
