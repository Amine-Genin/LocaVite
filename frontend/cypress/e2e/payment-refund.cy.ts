describe('Gestion des remboursements administrateur (/admin/payments)', () => {
  const mockPayments = [
    {
      id: 1,
      code: 'PAY-2026-001',
      reservationId: 101,
      clientNom: 'Karim Alami',
      userId: 2,
      montant: 1200.0,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      createdAt: '2026-11-15T10:00:00Z',
      paidAt: '2026-11-15T10:05:00Z',
    },
    {
      id: 2,
      code: 'PAY-2026-002',
      reservationId: 102,
      clientNom: 'Sara Bennani',
      userId: 3,
      montant: 800.0,
      devise: 'MAD',
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
      createdAt: '2026-11-15T11:00:00Z',
    },
  ];

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', /\/api\/payments(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: mockPayments,
        totalElements: mockPayments.length,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    }).as('getAdminPayments');
  });

  it('refuse l’accès au dashboard des paiements aux utilisateurs sans rôle ADMIN', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/admin/payments');
    cy.url().should('not.include', '/admin/payments');
  });

  it('affiche le tableau des transactions et le bouton de remboursement pour les paiements réussis', () => {
    cy.openAdminPayments();
    cy.openRefundDialog();
    cy.confirmRefund('Annulation client sous 48h');
    cy.wait('@refundTotal');
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
    cy.contains('PAY-2026-001').should('exist');
    cy.visit('/admin/payments');
    cy.wait('@getAdminPayments');

    cy.get('[data-testid="admin-payments-page"]').should('be.visible');
    cy.get('[data-testid="payments-table"]').should('be.visible');
    cy.contains('PAY-2026-001').should('be.visible');
    cy.contains('Karim Alami').should('be.visible');

    // Le bouton de remboursement est visible sur le paiement SUCCESS
    cy.get('[data-testid="btn-refund"]').should('be.visible');
  });

  it('permet d’ouvrir et d’annuler la boîte de dialogue de remboursement sans action', () => {
    cy.openAdminPayments();
    cy.openRefundDialog();
    cy.confirmRefund('Annulation client sous 48h');
    cy.wait('@refundTotal');
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
    cy.contains('PAY-2026-001').should('exist');
    cy.visit('/admin/payments');
    cy.wait('@getAdminPayments');

    cy.get('[data-testid="btn-refund"]').click();

    // Vérification de la présence de la modale
    cy.get('[data-testid="refund-dialog"]').should('be.visible');
    cy.contains('Confirmer le Remboursement').should('be.visible');
    cy.get('[data-testid="refund-dialog"]').within(() => {
      cy.contains('PAY-2026-001').should('be.visible');
    });

    // Clic sur Annuler
    cy.get('[data-testid="btn-cancel-refund"]').click();
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
  });

  it('effectue un remboursement total (FULL) avec motif justificatif', () => {
    cy.intercept('POST', '/api/admin/payments/1/refund', (req) => {
      expect(req.body.raison).to.eq('Annulation client sous 48h');
      req.reply({
        statusCode: 200,
        body: {
          refundId: 10,
          paymentId: 1,
          montantRembourse: 1200.0,
          statut: 'REFUNDED',
          refundedAt: '2026-11-18T10:30:00Z',
        },
      });
    }).as('refundTotal');

    cy.openAdminPayments();
    cy.openRefundDialog();
    cy.confirmRefund('Annulation client sous 48h');
    cy.wait('@refundTotal');
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
    cy.contains('PAY-2026-001').should('exist');
    cy.visit('/admin/payments');
    cy.wait('@getAdminPayments');

    cy.get('[data-testid="btn-refund"]').click();
    cy.get('[data-testid="refund-dialog"]').should('be.visible');

    // Saisie du motif obligatoire
    cy.get('[data-testid="refund-reason-input"]').type('Annulation client sous 48h');

    // Soumission du remboursement
    cy.get('[data-testid="btn-confirm-refund"]').should('not.be.disabled').click();
    cy.wait('@refundTotal');

    // Vérification de la notification snackbar
    cy.contains('PAY-2026-001').should('exist');
  });

  it('effectue un remboursement partiel (PARTIAL) avec montant et motif', () => {
    cy.intercept('POST', '/api/admin/payments/1/refund', (req) => {
      expect(req.body.montant).to.eq(400);
      expect(req.body.raison).to.eq('Geste commercial retard livraison');
      req.reply({
        statusCode: 200,
        body: {
          refundId: 11,
          paymentId: 1,
          montantRembourse: 400.0,
          statut: 'PARTIALLY_REFUNDED',
          refundedAt: '2026-11-18T10:35:00Z',
        },
      });
    }).as('refundPartial');

    cy.openAdminPayments();
    cy.openRefundDialog();
    cy.confirmRefund('Annulation client sous 48h');
    cy.wait('@refundTotal');
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
    cy.contains('PAY-2026-001').should('exist');
    cy.visit('/admin/payments');
    cy.wait('@getAdminPayments');

    cy.get('[data-testid="btn-refund"]').click();
    cy.get('[data-testid="refund-dialog"]').should('be.visible');

    // Basculer en mode Partiel
    cy.contains('Remboursement Partiel').click();
    cy.get('[data-testid="refund-amount-input"]').clear().type('400');
    cy.get('[data-testid="refund-reason-input"]').type('Geste commercial retard livraison');

    cy.get('[data-testid="btn-confirm-refund"]').should('not.be.disabled').click();
    cy.wait('@refundPartial');

    // La boîte de dialogue se ferme
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
  });
});
