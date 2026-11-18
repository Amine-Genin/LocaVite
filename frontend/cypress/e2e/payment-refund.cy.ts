describe('Gestion des remboursements administrateur (/admin/payments)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();
    cy.mockAdminPayments();
  });

  it('refuse l’accès au dashboard des paiements aux utilisateurs sans rôle ADMIN', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/admin/payments');
    cy.url().should('not.include', '/admin/payments');
  });

  it('affiche le tableau des transactions et le bouton de remboursement pour les paiements réussis', () => {
    cy.openAdminPayments();

    cy.get('[data-testid="admin-payments-page"]').should('be.visible');
    cy.get('[data-testid="payments-table"]').should('be.visible');
    cy.contains('PAY-2026-001').should('be.visible');
    cy.contains('Karim Alami').should('be.visible');

    // Le bouton de remboursement est visible sur le paiement SUCCESS
    cy.get('[data-testid="btn-refund"]').should('be.visible');
  });

  it('permet d’ouvrir et d’annuler la boîte de dialogue de remboursement sans action', () => {
    cy.openAdminPayments();

    cy.get('[data-testid="btn-refund"]').first().should('be.visible').click();

    // Vérification de la présence de la modale
    cy.get('[data-testid="refund-dialog"]').should('be.visible');
    cy.contains('Confirmer le Remboursement').should('be.visible');
    cy.get('[data-testid="refund-dialog"]').within(() => {
      cy.contains('PAY-2026-001').should('be.visible');
    });

    // Clic sur Annuler
    cy.get('[data-testid="btn-cancel-refund"]').should('be.visible').click();
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

    cy.get('[data-testid="btn-refund"]').first().should('be.visible').click();
    cy.get('[data-testid="refund-dialog"]').should('be.visible');

    // Saisie du motif obligatoire dans la modale
    cy.get('[data-testid="refund-dialog"]').within(() => {
      cy.get('[data-testid="refund-reason-input"]')
        .should('be.visible')
        .clear()
        .type('Annulation client sous 48h');
      cy.get('[data-testid="btn-confirm-refund"]').should('be.visible').and('not.be.disabled').click();
    });

    cy.wait('@refundTotal', { timeout: 10000 });
    cy.get('[data-testid="refund-dialog"]', { timeout: 10000 }).should('not.exist');
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

    cy.get('[data-testid="btn-refund"]').first().should('be.visible').click();
    cy.get('[data-testid="refund-dialog"]').should('be.visible');

    // Basculer en mode Partiel
    cy.contains('Remboursement Partiel').should('be.visible').click();
    cy.get('[data-testid="refund-dialog"]').within(() => {
      cy.get('[data-testid="refund-amount-input"]').should('be.visible').clear().type('400');
      cy.get('[data-testid="refund-reason-input"]')
        .should('be.visible')
        .clear()
        .type('Geste commercial retard livraison');
      cy.get('[data-testid="btn-confirm-refund"]').should('be.visible').and('not.be.disabled').click();
    });

    cy.wait('@refundPartial', { timeout: 10000 });
    cy.get('[data-testid="refund-dialog"]', { timeout: 10000 }).should('not.exist');
  });
});
