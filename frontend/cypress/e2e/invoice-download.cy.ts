describe('Téléchargement de facture PDF (Succès, Espace client, Admin)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();
  });

  it('permet au client de télécharger sa facture PDF depuis la page de confirmation (/payment/success)', () => {
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
        paidAt: '2026-11-18T10:00:00Z',
      },
    }).as('getPaymentSuccess');

    cy.intercept('GET', '/api/payments/42/invoice*', {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'attachment; filename="FAC-2026-00042.pdf"',
      },
      body: '%PDF-1.4 Mock PDF Invoice Content',
    }).as('downloadInvoiceSuccessPage');

    cy.visit('/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI');
    cy.wait('@getPaymentSuccess');

    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
    cy.get('[data-cy="download-invoice-btn"]').should('be.visible').click();

    cy.wait('@downloadInvoiceSuccessPage').then((interception) => {
      expect(interception.response?.statusCode).to.eq(200);
      const contentType =
        interception.response?.headers['content-type'] ||
        interception.response?.headers['Content-Type'];
      expect(contentType).to.include('application/pdf');
    });
  });

  it('affiche un message d’erreur convivial si le téléchargement de facture échoue', () => {
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
        paidAt: '2026-11-18T10:00:00Z',
      },
    }).as('getPaymentStatus');

    cy.intercept('GET', '/api/payments/42/invoice*', {
      statusCode: 500,
      body: { message: 'Génération PDF indisponible' },
    }).as('downloadInvoiceFail');

    cy.visit('/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI');
    cy.wait('@getPaymentStatus');

    cy.get('[data-cy="download-invoice-btn"]').click();
    cy.wait('@downloadInvoiceFail');

    // Message d'erreur affiché
    cy.get('[data-cy="download-invoice-error"]').should('be.visible');
    cy.get('[data-cy="download-invoice-btn"]').should('not.be.disabled');
  });

  it('permet au client de télécharger sa facture depuis son historique de paiements (/my-payments)', () => {
    const mockClientPayments = [
      {
        id: 101,
        code: 'PAY-CMI101',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CMI',
        statut: 'SUCCESS',
        createdAt: '2026-11-15T10:30:00Z',
        paidAt: '2026-11-15T10:31:00Z',
      },
    ];

    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('GET', /\/api\/payments\/me.*/, {
      statusCode: 200,
      body: {
        content: mockClientPayments,
        totalElements: 1,
        totalPages: 1,
        size: 50,
        number: 0,
      },
    }).as('getMyPayments');

    cy.intercept('GET', '/api/payments/101/invoice*', {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/pdf',
      },
      body: '%PDF-1.4 Mock PDF Invoice 101',
    }).as('downloadInvoiceMyPayments');

    cy.visit('/my-payments');
    cy.wait('@getMyPayments');

    cy.get('[data-cy="payments-table"]').should('be.visible');
    cy.get('[data-cy="download-invoice-101"]').should('be.visible').click();

    cy.wait('@downloadInvoiceMyPayments');
  });

  it('permet à l’administrateur d’extraire la facture PDF depuis le tableau de bord (/admin/payments)', () => {
    cy.intercept('GET', '/api/payments/1/invoice*', {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/pdf',
      },
      body: '%PDF-1.4 Mock PDF Invoice Admin',
    }).as('downloadInvoiceAdmin');

    cy.openAdminPayments();

    cy.get('[data-testid="btn-download-invoice"]').should('be.visible').click();
    cy.wait('@downloadInvoiceAdmin');
  });
});
