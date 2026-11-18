describe('Parcours de paiement en espèces (CASH_DELIVERY, CASH_AGENCY)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();
    cy.mockReservation(88);
  });

  const cashScenarios = [
    {
      method: 'CASH_DELIVERY',
      title: 'à la livraison (CASH_DELIVERY)',
      cardSelector: '[data-cy="method-cash-delivery-card"]',
      paymentId: 51,
      paymentCode: 'PAY-CSH051',
      invoiceNumber: 'FAC-2026-00051',
    },
    {
      method: 'CASH_AGENCY',
      title: 'en agence (CASH_AGENCY)',
      cardSelector: '[data-cy="method-cash-agency-card"]',
      paymentId: 52,
      paymentCode: 'PAY-CSH052',
    },
  ];

  cashScenarios.forEach((sc) => {
    it(`permet de choisir le paiement en espèces ${sc.title}`, () => {
      cy.loginAsClient('ahmed.benali@locavite.ma');

      cy.intercept('POST', '/api/payments/cash', (req) => {
        expect(req.body.reservationId).to.eq(88);
        expect(req.body.montant).to.eq(1400);
        expect(req.body.methode).to.eq(sc.method);
        req.reply({
          statusCode: 200,
          body: {
            id: sc.paymentId,
            code: sc.paymentCode,
            reservationId: 88,
            montant: 1400.0,
            devise: 'MAD',
            methode: sc.method,
            statut: 'PENDING',
            createdAt: '2026-11-18T10:00:00Z',
          },
        });
      }).as('payCash');

      cy.intercept('GET', `/api/payments/${sc.paymentId}`, {
        statusCode: 200,
        body: {
          id: sc.paymentId,
          code: sc.paymentCode,
          reservationId: 88,
          montant: 1400.0,
          devise: 'MAD',
          methode: sc.method,
          statut: 'PENDING',
          createdAt: '2026-11-18T10:00:00Z',
        },
      }).as('getStatus');

      cy.visit('/payment/88');
      cy.wait('@getReservation');

      cy.get(sc.cardSelector).click();
      cy.get(sc.cardSelector).should('have.class', 'selected');

      cy.get('[data-cy="submit-payment-btn"]').click();
      cy.wait('@payCash');

      cy.url().should('include', '/payment/success');
      cy.url().should('include', `method=${sc.method}`);
      cy.wait('@getStatus');

      cy.get('[data-cy="payment-success-page"]').should('be.visible');
      cy.get('[data-cy="payment-code"]').should('contain.text', sc.paymentCode);
      if (sc.invoiceNumber) {
        cy.get('[data-cy="invoice-number"]').should('contain.text', sc.invoiceNumber);
      }
    });
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

    cy.loginAdmin();
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
