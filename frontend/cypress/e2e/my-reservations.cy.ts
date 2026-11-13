describe('Gestion des réservations client (/my-reservations)', () => {
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

  it('redirige vers /login si le client n’est pas authentifié', () => {
    cy.visit('/my-reservations');
    cy.url().should('include', '/login');
  });

  it('affiche les réservations organisées par onglets avec détails et statuts', () => {
    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Vérifie l'onglet actif par défaut "À venir" (statut PENDING / CONFIRMED)
    cy.get('[data-testid="reservations-list"]').should('be.visible');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'LV-TEST01');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'Dacia Logan');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'En attente');
    cy.get('[data-testid="price-total-101"]').should('contain.text', '1,500.00 MAD');

    // Onglet "En cours" (statut ACTIVE)
    cy.contains('.mat-mdc-tab', 'En cours').click();
    cy.get('[data-testid="reservation-card-102"]').should('be.visible');
    cy.get('[data-testid="reservation-card-102"]').should('contain.text', 'LV-TEST02');
    cy.get('[data-testid="reservation-card-102"]').should('contain.text', 'En cours');

    // Onglet "Passées" (statut COMPLETED)
    cy.contains('.mat-mdc-tab', 'Passées').click();
    cy.get('[data-testid="reservation-card-103"]').should('be.visible');
    cy.get('[data-testid="reservation-card-103"]').should('contain.text', 'LV-TEST03');
    cy.get('[data-testid="reservation-card-103"]').should('contain.text', 'Terminée');

    // Onglet "Annulées" (statut CANCELLED)
    cy.contains('.mat-mdc-tab', 'Annulées').click();
    cy.get('[data-testid="reservation-card-104"]').should('be.visible');
    cy.get('[data-testid="reservation-card-104"]').should('contain.text', 'LV-TEST04');
    cy.get('[data-testid="reservation-card-104"]').should('contain.text', 'Annulée');
  });

  it('affiche un état vide et un lien vers le catalogue si aucune réservation n’existe', () => {
    cy.intercept('GET', /\/api\/reservations\/me.*/, {
      statusCode: 200,
      body: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
      },
    }).as('getEmptyReservations');

    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getEmptyReservations');

    cy.get('[data-testid="empty-reservations"]').should('be.visible');
    cy.contains('Aucune réservation trouvée').should('be.visible');
    cy.contains('Explorer le catalogue').should('have.attr', 'href', '/vehicles');
  });

  it('affiche les actions autorisées selon le statut de la réservation', () => {
    cy.loginAsClient();
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Réservation PENDING (101) : Voir détails et Annuler présents
    cy.get('[data-testid="reservation-card-101"]').within(() => {
      cy.get('[data-testid="btn-view-details"]').should('be.visible');
      cy.get('[data-testid="btn-cancel-reservation"]').should('be.visible');
    });

    // Réservation COMPLETED (103) : Pas de bouton Annuler
    cy.contains('.mat-mdc-tab', 'Passées').click();
    cy.get('[data-testid="reservation-card-103"]').within(() => {
      cy.get('[data-testid="btn-view-details"]').should('be.visible');
      cy.get('[data-testid="btn-cancel-reservation"]').should('not.exist');
    });
  });
});
