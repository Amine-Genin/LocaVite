describe('Gestion des échecs de paiement et parcours de nouvelle tentative (/payment/failure, retry)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();
    cy.mockReservation(88);
  });

  it('affiche un bandeau d’erreur explicite si l’initiation CMI échoue (ex: 503 Gateway Timeout)', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('POST', '/api/payments/cmi/initiate', {
      statusCode: 503,
      body: {
        message: 'Passerelle CMI momentanément indisponible. Veuillez réessayer.',
      },
    }).as('initiateCmiFail');

    cy.visit('/payment/88');
    cy.wait('@getReservation');

    cy.get('[data-cy="method-cmi-card"]').click();
    cy.get('[data-cy="submit-payment-btn"]').click();
    cy.wait('@initiateCmiFail');

    // Vérification du message d'erreur
    cy.get('[data-cy="payment-error-banner"]').should('be.visible');
    cy.get('[data-cy="payment-error-text"]').should('contain.text', 'Passerelle CMI momentanément indisponible');
    cy.get('[data-cy="submit-payment-btn"]').should('not.be.disabled');
  });

  it('affiche la page /payment/failure avec motif d’échec et permet de retenter le paiement', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.visit('/payment/failure?reservationId=88&paymentId=42&error=Transaction%20refus%C3%A9e%20par%20la%20banque');

    // Vérification des éléments de la page d'échec
    cy.get('[data-cy="payment-failure-page"]').should('be.visible');
    cy.get('[data-cy="failure-icon"]').should('be.visible');
    cy.get('[data-cy="payment-failure-title"]').should('be.visible');
    cy.get('[data-cy="payment-failure-reason"]').should('contain.text', 'Transaction refusée par la banque');

    // Clic sur Réessayer le paiement
    cy.get('[data-cy="retry-payment-btn"]').should('be.visible').click();

    // Redirection vers /payment/88
    cy.url().should('include', '/payment/88');
  });

  it('redirige automatiquement vers /payment/failure si le polling CMI retourne un statut FAILED', () => {
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
        statut: 'FAILED',
      },
    }).as('getFailedPayment');

    cy.visit('/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI');
    cy.wait('@getFailedPayment');

    // Doit basculer vers /payment/failure
    cy.url().should('include', '/payment/failure');
    cy.get('[data-cy="payment-failure-page"]').should('be.visible');
  });

  it('permet de compléter avec succès le paiement lors d’une nouvelle tentative après échec', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    // 1. Départ sur la page d'échec
    cy.visit('/payment/failure?reservationId=88&paymentId=42&error=Solde%20insuffisant');
    cy.get('[data-cy="retry-payment-btn"]').click();
    cy.url().should('include', '/payment/88');
    cy.wait('@getReservation');

    // 2. Cette fois, l'initiation et le paiement réussissent
    cy.fixture('payment-cmi-success.json').then((cmiFixture) => {
      cy.intercept('POST', '/api/payments/cmi/initiate', {
        statusCode: 200,
        body: cmiFixture,
      }).as('initiateCmiSuccess');
    });

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
        paidAt: '2026-11-18T10:15:00Z',
      },
    }).as('getSuccessPayment');

    cy.get('[data-cy="method-cmi-card"]').click();
    cy.get('[data-cy="submit-payment-btn"]').click();
    cy.wait('@initiateCmiSuccess');

    cy.url().should('include', '/payment/success');
    cy.wait('@getSuccessPayment');
    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
  });
});
