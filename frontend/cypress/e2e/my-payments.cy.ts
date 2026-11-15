describe('Historique des paiements client (/my-payments)', () => {
  const mockPayments = [
    {
      id: 101,
      code: 'PAY-CMI101',
      reservationId: 88,
      montant: 1400.0,
      devise: 'MAD',
      methode: 'CMI',
      statut: 'SUCCESS',
      createdAt: '2026-11-10T10:30:00Z',
      paidAt: '2026-11-10T10:31:00Z',
    },
    {
      id: 102,
      code: 'PAY-CSH102',
      reservationId: 89,
      montant: 900.0,
      devise: 'MAD',
      methode: 'CASH_AGENCY',
      statut: 'PENDING',
      createdAt: '2026-11-12T14:00:00Z',
    },
    {
      id: 103,
      code: 'PAY-VIR103',
      reservationId: 90,
      montant: 2100.0,
      devise: 'MAD',
      methode: 'BANK_TRANSFER',
      statut: 'REFUNDED',
      createdAt: '2026-11-14T09:15:00Z',
      paidAt: '2026-11-14T09:20:00Z',
    },
  ];

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.intercept('GET', /\/api\/payments\/me.*/, {
      statusCode: 200,
      body: {
        content: mockPayments,
        totalElements: mockPayments.length,
        totalPages: 1,
        size: 50,
        number: 0,
      },
    }).as('getMyPayments');
  });

  it('redirige vers /login si le client n’est pas connecté', () => {
    cy.visit('/my-payments');
    cy.url().should('include', '/login');
  });

  it('Test : tableau affiche les paiements, le total en haut et les badges de statut colorés', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/my-payments');
    cy.wait('@getMyPayments');

    // Vérification du total en haut (1400 + 900 + 2100 = 4400.00 MAD)
    cy.get('[data-cy="total-payments-amount"]').should('contain.text', '4,400.00 MAD');
    cy.get('[data-cy="total-payments-count"]').should('contain.text', '3');

    // Vérification des lignes du tableau
    cy.get('[data-cy="payments-table"]').should('be.visible');
    cy.get('[data-testid="payment-row"]').should('have.length', 3);

    // Vérification des badges colorés
    cy.get('[data-cy="status-badge-SUCCESS"]').should('have.class', 'status-success');
    cy.get('[data-cy="status-badge-PENDING"]').should('have.class', 'status-pending');
    cy.get('[data-cy="status-badge-REFUNDED"]').should('have.class', 'status-refunded');
  });

  it('Test : filtre par statut, méthode et date met à jour le tableau et le total', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/my-payments');
    cy.wait('@getMyPayments');

    // Filtrage par statut SUCCESS
    cy.get('[data-cy="filter-status"]').click();
    cy.get('[data-cy="filter-status-SUCCESS"]').click();

    cy.get('[data-testid="payment-row"]').should('have.length', 1);
    cy.get('[data-cy="payment-row-101"]').should('be.visible');
    cy.get('[data-cy="total-payments-amount"]').should('contain.text', '1,400.00 MAD');

    // Réinitialisation des filtres
    cy.get('[data-cy="reset-filters-btn"]').click();
    cy.get('[data-testid="payment-row"]').should('have.length', 3);
  });

  it('Test : téléchargement de la facture PDF et navigation vers la réservation associée', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    cy.intercept('GET', '/api/payments/101/invoice*', {
      statusCode: 200,
      headers: {
        'Content-Type': 'application/pdf',
      },
      body: '%PDF-1.4 mock invoice 101',
    }).as('downloadInvoice101');

    cy.visit('/my-payments');
    cy.wait('@getMyPayments');

    // Téléchargement de la facture PDF
    cy.get('[data-cy="download-invoice-101"]').click();
    cy.wait('@downloadInvoice101');

    // Navigation vers la réservation associée (#88)
    cy.get('[data-cy="view-reservation-101"]').click();
    cy.url().should('include', '/reservations/88');
  });
});
