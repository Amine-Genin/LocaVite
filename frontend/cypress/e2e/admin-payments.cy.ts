describe('Dashboard Paiements Admin (/admin/payments)', () => {
  const mockPayments = [
    {
      id: 1,
      code: 'PAY-2026-001',
      codePaiement: 'PAY-2026-001',
      reservationId: 101,
      clientNom: 'Karim Alami',
      clientEmail: 'karim@test.ma',
      montant: 1200.0,
      methode: 'CARTE',
      statut: 'SUCCESS',
      dateCreation: '2026-11-15T10:00:00',
      datePaiement: '2026-11-15T10:05:00',
      transactionId: 'TXN-001',
      motifRemboursement: null,
      montantRembourse: null,
    },
    {
      id: 2,
      code: 'PAY-2026-002',
      codePaiement: 'PAY-2026-002',
      reservationId: 102,
      clientNom: 'Sara Bennani',
      clientEmail: 'sara@test.ma',
      montant: 800.0,
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
      dateCreation: '2026-11-15T11:00:00',
      datePaiement: null,
      transactionId: null,
      motifRemboursement: null,
      montantRembourse: null,
    },
    {
      id: 3,
      code: 'PAY-2026-003',
      codePaiement: 'PAY-2026-003',
      reservationId: 103,
      clientNom: 'Omar Mansour',
      clientEmail: 'omar@test.ma',
      montant: 500.0,
      methode: 'VIREMENT',
      statut: 'REFUNDED',
      dateCreation: '2026-11-14T09:00:00',
      datePaiement: '2026-11-14T09:30:00',
      transactionId: 'TXN-003',
      motifRemboursement: 'Annulation réservation',
      montantRembourse: 500.0,
    },
  ];

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        email: 'admin@locavite.ma',
        nom: 'Admin',
        prenom: 'System',
        role: 'ADMIN',
      },
    }).as('getProfile');

    cy.intercept('GET', /\/api\/payments(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: mockPayments,
        totalElements: mockPayments.length,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    }).as('getPayments');
  });

  it('refuse l’accès aux clients réguliers et redirige vers accueil', () => {
    cy.loginAsClient();
    cy.visit('/admin/payments');
    cy.url().should('not.include', '/admin/payments');
  });

  it('affiche le dashboard de paiements avec stats, graphiques et tableau complet', () => {
    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getPayments');

    // Titre et header
    cy.contains('Gestion des Paiements').should('be.visible');

    // Cartes KPIs
    cy.contains('Revenus du mois').should('be.visible');
    cy.contains(/1[,\s]200/).should('be.visible');
    cy.contains('Transactions').should('be.visible');
    cy.contains('Taux de succès').should('be.visible');

    // Tableau complet
    cy.get('table').should('be.visible');
    cy.contains('PAY-2026-001').should('be.visible');
    cy.contains('Karim Alami').should('be.visible');
    cy.contains('PAY-2026-002').should('be.visible');
    cy.contains('Sara Bennani').should('be.visible');
    cy.contains('PAY-2026-003').should('be.visible');
  });

  it('permet de filtrer et rechercher dans la liste des paiements', () => {
    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getPayments');

    // Recherche par client ou code
    cy.get('[data-testid="filter-search-input"]').type('Sara', { force: true });
    cy.contains('Sara Bennani').should('be.visible');
    cy.contains('Karim Alami').should('not.exist');

    // Effacer recherche
    cy.get('[data-testid="filter-search-input"]').clear({ force: true });
    cy.contains('Karim Alami').should('be.visible');
  });

  it('permet de confirmer un paiement en espèces en attente', () => {
    cy.intercept('PUT', '/api/admin/payments/2/confirm-cash', {
      statusCode: 200,
      body: {
        ...mockPayments[1],
        statut: 'SUCCESS',
        paidAt: '2026-11-15T12:00:00',
      },
    }).as('confirmCash');

    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getPayments');

    // Cliquer sur le bouton de validation cash pour PAY-2026-002
    cy.on('window:confirm', () => true);
    cy.get('[data-testid="btn-confirm-cash"]').should('be.visible').click();
    cy.wait('@confirmCash');
    cy.contains('PAY-2026-002').should('be.visible');
  });

  it('permet d’ouvrir le dialogue de remboursement', () => {
    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getPayments');

    // Ouvrir le dialogue de remboursement pour PAY-2026-001 (statut SUCCESS)
    cy.get('[data-testid="btn-refund"]').first().should('be.visible').click();
    cy.get('[data-testid="refund-dialog"]').should('be.visible');
    cy.contains('Confirmer le Remboursement').should('be.visible');

    // Annuler la boîte de dialogue
    cy.get('[data-testid="btn-cancel-refund"]').click();
    cy.get('[data-testid="refund-dialog"]').should('not.exist');
  });

  it('permet d’exporter la liste en CSV', () => {
    cy.login('admin@locavite.ma', 'Motdepasse123!', ['ADMIN']);
    cy.visit('/admin/payments');
    cy.wait('@getPayments');

    cy.contains('button', 'Exporter CSV').should('be.visible').click();
  });
});
