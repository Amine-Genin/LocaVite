describe('Mes réservations (/my-reservations)', () => {
  const mockReservations = [
    {
      id: 101,
      code: 'LV-TEST01',
      statut: 'PENDING',
      dateDebut: '2026-11-20',
      dateFin: '2026-11-25',
      prixTotal: 1500,
      caution: 3000,
      lieuPriseEnCharge: 'Aéroport Casablanca',
      vehicleId: 1,
      vehicle: {
        id: 1,
        marque: 'Dacia',
        modele: 'Logan',
        annee: 2023,
        categorie: 'Économique',
        ville: 'Casablanca',
        prixJour: 250,
      },
    },
    {
      id: 102,
      code: 'LV-TEST02',
      statut: 'ACTIVE',
      dateDebut: '2026-11-10',
      dateFin: '2026-11-15',
      prixTotal: 3000,
      caution: 5000,
      lieuPriseEnCharge: 'Gare Rabat Ville',
      vehicleId: 2,
      vehicle: {
        id: 2,
        marque: 'Renault',
        modele: 'Clio',
        annee: 2024,
        categorie: 'Citadine',
        ville: 'Rabat',
        prixJour: 350,
      },
    },
    {
      id: 103,
      code: 'LV-TEST03',
      statut: 'COMPLETED',
      dateDebut: '2026-10-01',
      dateFin: '2026-10-05',
      prixTotal: 1200,
      caution: 3000,
      vehicleId: 1,
    },
    {
      id: 104,
      code: 'LV-TEST04',
      statut: 'CANCELLED',
      dateDebut: '2026-09-10',
      dateFin: '2026-09-12',
      prixTotal: 600,
      caution: 2000,
      vehicleId: 2,
    },
  ];

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 2,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
      },
    }).as('getProfile');

    cy.intercept('GET', /\/api\/reservations\/me.*/, {
      statusCode: 200,
      body: {
        content: mockReservations,
        totalElements: 4,
        totalPages: 1,
        size: 20,
        number: 0,
      },
    }).as('getMyReservations');
  });

  it('redirige vers /login si le client n’est pas connecté', () => {
    cy.visit('/my-reservations');
    cy.url().should('include', '/login');
  });

  it('affiche les réservations par onglet avec leurs détails', () => {
    cy.login('ahmed.benali@locavite.ma', 'Motdepasse123!', ['CLIENT']);
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Vérifie l'onglet actif par défaut "À venir"
    cy.get('[data-testid="reservations-list"]').should('be.visible');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'LV-TEST01');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'Dacia Logan');
    cy.get('[data-testid="reservation-card-101"]').should('contain.text', 'En attente');

    // Cliquer sur l'onglet "En cours"
    cy.contains('.mat-mdc-tab', 'En cours').click();
    cy.get('[data-testid="reservation-card-102"]').should('be.visible');
    cy.get('[data-testid="reservation-card-102"]').should('contain.text', 'LV-TEST02');
    cy.get('[data-testid="reservation-card-102"]').should('contain.text', 'En cours');

    // Cliquer sur l'onglet "Passées"
    cy.contains('.mat-mdc-tab', 'Passées').click();
    cy.get('[data-testid="reservation-card-103"]').should('be.visible');
    cy.get('[data-testid="reservation-card-103"]').should('contain.text', 'LV-TEST03');

    // Cliquer sur l'onglet "Annulées"
    cy.contains('.mat-mdc-tab', 'Annulées').click();
    cy.get('[data-testid="reservation-card-104"]').should('be.visible');
    cy.get('[data-testid="reservation-card-104"]').should('contain.text', 'Annulée');
  });

  it('permet d’annuler une réservation éligible après confirmation', () => {
    cy.intercept('DELETE', '/api/reservations/101', {
      statusCode: 204,
    }).as('cancelReservation');

    cy.login('ahmed.benali@locavite.ma', 'Motdepasse123!', ['CLIENT']);
    cy.visit('/my-reservations');
    cy.wait('@getMyReservations');

    // Accepter la boîte de confirmation window.confirm
    cy.on('window:confirm', () => true);

    cy.get('[data-testid="reservation-card-101"]')
      .find('[data-testid="btn-cancel-reservation"]')
      .click();

    cy.wait('@cancelReservation');
    cy.contains('Réservation annulée avec succès.').should('be.visible');
  });

  it('affiche un état vide si aucune réservation n’est présente', () => {
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

    cy.login('ahmed.benali@locavite.ma', 'Motdepasse123!', ['CLIENT']);
    cy.visit('/my-reservations');
    cy.wait('@getEmptyReservations');

    cy.get('[data-testid="empty-reservations"]').should('be.visible');
    cy.contains('Aucune réservation trouvée').should('be.visible');
    cy.contains('Explorer le catalogue').should('have.attr', 'href', '/vehicles');
  });
});
