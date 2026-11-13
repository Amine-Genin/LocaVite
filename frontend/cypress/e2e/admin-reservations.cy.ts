describe('Gestion admin des réservations (/admin/reservations)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.fixture('reservations.json').then((resData) => {
      cy.intercept('GET', '/api/users/me', {
        statusCode: 200,
        body: resData.agencyUser,
      }).as('getProfile');

      cy.intercept('GET', /\/api\/reservations(\?.*)?$/, {
        statusCode: 200,
        body: {
          content: resData.adminReservations,
          totalElements: resData.adminReservations.length,
          totalPages: 1,
          size: 10,
          number: 0,
        },
      }).as('getAllReservations');
    });
  });

  it('refuse l’accès aux clients non autorisés et redirige', () => {
    cy.loginAsClient();
    cy.visit('/admin/reservations');
    cy.url().should('not.include', '/admin/reservations');
  });

  it('affiche le dashboard administrateur, les indicateurs clés et le tableau des réservations', () => {
    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    // Cartes statistiques en haut de page
    cy.get('[data-testid="stats-cards-container"]').should('be.visible');
    cy.get('[data-testid="stat-total"]').should('contain.text', '2');

    // Tableau des réservations
    cy.get('[data-testid="admin-reservations-table"]').should('be.visible');
    cy.contains('LV-ADM01').should('be.visible');
    cy.contains('Karim Idrissi').should('be.visible');
    cy.contains('Peugeot 208').should('be.visible');

    // Bouton de confirmation présent pour une réservation PENDING
    cy.get('[data-testid="btn-confirm-201"]').should('be.visible');
  });

  it('permet à l’agence de confirmer une réservation PENDING avec notification de succès', () => {
    cy.fixture('reservations.json').then((resData) => {
      cy.intercept('PATCH', '/api/reservations/201/confirm', {
        statusCode: 200,
        body: { ...resData.adminReservations[0], statut: 'CONFIRMED' },
      }).as('confirmReservation');

      cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
      cy.visit('/admin/reservations');
      cy.wait('@getAllReservations');

      cy.get('[data-testid="btn-confirm-201"]').click();
      cy.wait('@confirmReservation');
      cy.contains('confirmée avec succès').should('be.visible');
    });
  });

  it('permet de filtrer les réservations par code', () => {
    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    cy.get('[data-testid="filter-code-input"]').type('LV-ADM01', { force: true });
    cy.get('[data-testid="btn-apply-filters"]').click({ force: true });

    cy.get('[data-testid="admin-reservations-table"]').should('contain.text', 'LV-ADM01');
  });

  it('permet d’accéder aux actions du menu contextuel (contrat PDF, gestion caution)', () => {
    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    cy.get('[data-testid="menu-actions-201"]').click();
    cy.contains('Voir contrat PDF').should('be.visible');
    cy.contains('Gérer la caution').should('be.visible');
    cy.contains('Annuler réservation').should('be.visible');
  });
});
