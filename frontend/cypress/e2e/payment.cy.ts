describe('Parcours de paiement en ligne (/payment/:reservationId, /payment/success, /payment/failure)', () => {
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
      carburant: 'DIESEL',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
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
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/reservations/88', {
      statusCode: 200,
      body: mockReservation,
    }).as('getReservation');
  });

  it('redirige vers /login si un visiteur non connecté accède à /payment/88', () => {
    cy.visit('/payment/88');
    cy.url().should('include', '/login');
  });

  it('affiche le récapitulatif de la réservation et les 4 modes de paiement disponibles (avec RIB pour virement)', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/payment/88');
    cy.wait('@getReservation');

    // Vérification du récapitulatif
    cy.get('[data-cy="reservation-summary-card"]').should('be.visible');
    cy.get('[data-cy="summary-vehicle"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-cy="summary-dates"]').should('contain.text', '01/12/2026');
    cy.get('[data-cy="summary-amount"]').should('contain.text', '1,400.00 MAD');

    // Vérification des 4 modes disponibles
    cy.get('[data-cy="method-cmi"]').should('be.visible');
    cy.get('[data-cy="method-cash-delivery"]').should('be.visible');
    cy.get('[data-cy="method-cash-agency"]').should('be.visible');
    cy.get('[data-cy="method-bank-transfer"]').should('be.visible');

    // Sélection Virement bancaire → affichage du RIB
    cy.get('[data-cy="method-bank-transfer-card"]').click();
    cy.get('[data-cy="bank-rib-card"]').should('be.visible');
    cy.get('[data-cy="rib-number"]').should('contain.text', '007 780 0001234567890123 45');
  });

  it('sélection CMI → initie le paiement CMI et déclenche la redirection vers la passerelle CMI', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('POST', '/api/payments/cmi/initiate', (req) => {
      expect(req.body.reservationId).to.eq(88);
      expect(req.body.montant).to.eq(1400);
      req.reply({
        statusCode: 200,
        body: {
          paymentId: 42,
          paymentCode: 'PAY-CMI042',
          montant: 1400.0,
          devise: 'MAD',
          gatewayUrl: '/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI',
          redirectUrl: '/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI',
          formData: {
            clientid: 'locavite_merchant',
            oid: 'PAY-CMI042',
            amount: '1400.00',
            hash: 'VALID_HMAC_SHA512==',
          },
        },
      });
    }).as('initiateCmi');

    cy.intercept('GET', '/api/payments/42', {
      statusCode: 200,
      body: {
        id: 42,
        code: 'PAY-CMI042',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CMI',
        statut: 'SUCCESS',
        paidAt: '2026-11-14T22:00:00Z',
      },
    }).as('getPaymentStatus');

    cy.visit('/payment/88');
    cy.wait('@getReservation');

    cy.get('[data-cy="method-cmi-card"]').click();
    cy.get('[data-cy="submit-payment-btn"]').click();

    cy.wait('@initiateCmi');
    cy.url().should('include', '/payment/success');
    cy.wait('@getPaymentStatus');

    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
    cy.get('[data-cy="download-invoice-btn"]').should('be.visible');
  });

  it('page /payment/success affiche le numéro de facture et permet de télécharger la facture PDF', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('GET', '/api/payments/42', {
      statusCode: 200,
      body: {
        id: 42,
        code: 'PAY-CMI042',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CMI',
        statut: 'SUCCESS',
        paidAt: '2026-11-14T22:00:00Z',
      },
    }).as('getPaymentStatus');

    cy.intercept('GET', '/api/payments/42/invoice*', {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/pdf',
      },
      body: '%PDF-1.4 mock invoice',
    }).as('downloadInvoice');

    cy.visit('/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI');
    cy.wait('@getPaymentStatus');

    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
    cy.get('[data-cy="download-invoice-btn"]').click();
    cy.wait('@downloadInvoice');
  });

  it('page /payment/failure affiche le message d’échec et permet de réessayer', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.visit('/payment/failure?reservationId=88&paymentId=42&error=Transaction%20refus%C3%A9e');
    cy.get('[data-cy="payment-failure-page"]').should('be.visible');
    cy.get('[data-cy="payment-failure-reason"]').should('contain.text', 'Transaction refusée');

    cy.get('[data-cy="retry-payment-btn"]').click();
    cy.url().should('include', '/payment/88');
  });
});
