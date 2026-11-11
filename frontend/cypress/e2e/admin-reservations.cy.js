describe('Dashboard réservations admin (/admin/reservations)', () => {
  const mockReservations = [
    {
      id: 201,
      code: 'LV-ADM01',
      statut: 'PENDING',
      dateDebut: '2026-11-20',
      dateFin: '2026-11-25',
      prixTotal: 1800,
      caution: 3500,
      vehicleId: 1,
      clientNom: 'Karim Idrissi',
      vehicle: {
        id: 1,
        marque: 'Peugeot',
        modele: '208',
        annee: 2023,
      },
    },
    {
      id: 202,
      code: 'LV-ADM02',
      statut: 'ACTIVE',
      dateDebut: '2026-11-10',
      dateFin: '2026-11-15',
      prixTotal: 2500,
      caution: 4000,
      vehicleId: 2,
      clientNom: 'Nadia Alaoui',
      vehicle: {
        id: 2,
        marque: 'Renault',
        modele: 'Clio',
        annee: 2024,
      },
    },
  ];

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        nom: 'Agence',
        prenom: 'Admin',
        email: 'agence@locavite.ma',
        roles: ['AGENCE'],
      },
    }).as('getProfile');

    cy.intercept('GET', /\/api\/reservations(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: mockReservations,
        totalElements: 2,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    }).as('getAllReservations');
  });

  it('refuse l’accès aux utilisateurs non autorisés', () => {
    cy.visit('/admin/reservations');
    cy.url().should('not.include', '/admin/reservations');
  });

  it('affiche le dashboard et le tableau pour une agence', () => {
    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    // Vérifie les stats en haut
    cy.get('[data-testid="stats-cards-container"]').should('be.visible');
    cy.get('[data-testid="stat-total"]').should('contain.text', '2');

    // Vérifie les lignes du tableau
    cy.get('[data-testid="admin-reservations-table"]').should('be.visible');
    cy.contains('LV-ADM01').should('be.visible');
    cy.contains('Karim Idrissi').should('be.visible');
    cy.contains('Peugeot 208').should('be.visible');

    // Bouton Confirmer présent pour PENDING
    cy.get('[data-testid="btn-confirm-201"]').should('be.visible');
  });

  it('confirme une réservation PENDING avec rafraîchissement', () => {
    cy.intercept('PATCH', '/api/reservations/201/confirm', {
      statusCode: 200,
      body: { ...mockReservations[0], statut: 'CONFIRMED' },
    }).as('confirmReservation');

    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    cy.get('[data-testid="btn-confirm-201"]').click();
    cy.wait('@confirmReservation');
    cy.contains('confirmée avec succès').should('be.visible');
  });

  it('filtre par statut et par code', () => {
    cy.login('agence@locavite.ma', 'Motdepasse123!', ['AGENCE']);
    cy.visit('/admin/reservations');
    cy.wait('@getAllReservations');

    cy.get('[data-testid="filter-code-input"]').type('LV-ADM01');
    cy.get('[data-testid="btn-apply-filters"]').click();

    cy.get('[data-testid="admin-reservations-table"]').should('contain.text', 'LV-ADM01');
  });
});
