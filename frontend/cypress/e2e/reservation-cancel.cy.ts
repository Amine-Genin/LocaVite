describe('Annulation de réservation (/my-reservations)', () => {
  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.fixture('reservations.json').then((resData) => {
      cy.intercept('GET', '/api/users/me', {
        statusCode: 200,
        body: resData.clientUser,
      }).as('getProfile');

      cy.intercept('GET', /\/api\/reservations\/me.*/, {
        statusCode: 200,
        body: {
          content: resData.myReservations,
          totalElements: resData.myReservations.length,
          totalPages: 1,
          size: 20,
          number: 0,
        },
      }).as('getMyReservations');
    });
  });

  it('permet au client d’annuler une réservation éligible après confirmation', () => {
    cy.intercept('DELETE', '/api/reservations/101', {
      statusCode: 204,
    }).as('cancelReservation');

    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Accepter le dialogue window.confirm
    cy.on('window:confirm', () => true);

    cy.get('[data-testid="reservation-card-101"]')
      .find('[data-testid="btn-cancel-reservation"]')
      .should('be.visible')
      .click();

    cy.wait('@cancelReservation');
    cy.contains('Réservation annulée avec succès.').should('be.visible');
  });

  it('n’annule pas la réservation si le client rejette la confirmation', () => {
    const cancelStub = cy.stub().as('cancelCall');
    cy.intercept('DELETE', '/api/reservations/101', cancelStub);

    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Refuser le dialogue window.confirm
    cy.on('window:confirm', () => false);

    cy.get('[data-testid="reservation-card-101"]')
      .find('[data-testid="btn-cancel-reservation"]')
      .click();

    cy.get('@cancelCall').should('not.have.been.called');
    cy.contains('Réservation annulée avec succès.').should('not.exist');
  });

  it('affiche un message d’erreur si l’annulation échoue côté serveur', () => {
    cy.intercept('DELETE', '/api/reservations/101', {
      statusCode: 400,
      body: { message: 'Délai d’annulation dépassé pour cette réservation.' },
    }).as('cancelFail');

    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    cy.on('window:confirm', () => true);

    cy.get('[data-testid="reservation-card-101"]')
      .find('[data-testid="btn-cancel-reservation"]')
      .click();

    cy.wait('@cancelFail');
    cy.contains("Échec de l'annulation de la réservation.").should('be.visible');
  });

  it('interdit l’annulation sur les réservations terminées', () => {
    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Aller sur l'onglet des réservations passées (COMPLETED)
    cy.contains('.mat-mdc-tab', 'Passées').click();
    cy.get('[data-testid="reservation-card-103"]').within(() => {
      cy.get('[data-testid="btn-cancel-reservation"]').should('not.exist');
    });
  });
});
