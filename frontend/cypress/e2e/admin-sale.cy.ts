describe('Admin Sale Listings Dashboard (/admin/sale)', () => {
  const mockAdminUser = {
    id: 1,
    nom: 'Admin',
    prenom: 'LocaVite',
    email: 'admin.dev@locavite.ma',
    roles: ['ADMIN'],
  };

  const mockListings = [
    {
      id: 1,
      vehicleId: 10,
      titre: 'Dacia Duster 2022',
      prixVente: 185000,
      prixNego: true,
      etat: 'USED',
      kilometrage: 45000,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2022,
      ville: 'Casablanca',
      carburant: 'DIESEL',
      categorie: 'SUV',
      photoUrl: null,
      statut: 'PUBLISHED',
      publishedAt: '2026-11-20T10:00:00Z',
    },
    {
      id: 2,
      vehicleId: 11,
      titre: 'Peugeot 208 GT',
      prixVente: 220000,
      prixNego: false,
      etat: 'NEW',
      kilometrage: 0,
      marque: 'Peugeot',
      modele: '208',
      annee: 2024,
      ville: 'Rabat',
      carburant: 'ESSENCE',
      categorie: 'CITADINE',
      photoUrl: null,
      statut: 'DRAFT',
      publishedAt: null,
    },
  ];

  beforeEach(() => {
    cy.clearLocalStorage();
    localStorage.setItem('auth_token', 'mock-valid-token');
    localStorage.setItem('auth_user', JSON.stringify(mockAdminUser));

    cy.intercept('GET', '/api/sale-listings?page=0&size=10', {
      statusCode: 200,
      body: {
        content: mockListings,
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 10,
      },
    }).as('getMyListings');

    cy.intercept('PUT', '/api/sale-listings/2/publish', {
      statusCode: 200,
      body: { ...mockListings[1], statut: 'PUBLISHED' },
    }).as('publishListing');

    cy.intercept('PUT', '/api/sale-listings/1/mark-sold', {
      statusCode: 200,
      body: { ...mockListings[0], statut: 'SOLD' },
    }).as('markSoldListing');
  });

  it('affiche le tableau des annonces de vente avec les colonnes attendues', () => {
    cy.visit('/admin/sale');
    cy.wait('@getMyListings');

    cy.get('[data-cy="admin-sale-page"]').should('be.visible');
    cy.get('[data-cy="admin-sale-table"]').should('be.visible');
    cy.get('[data-cy="listing-title"]').should('have.length', 2);
    cy.get('[data-cy="listing-title"]').first().should('contain.text', 'Dacia Duster');
  });

  it('permet de basculer entre vue tableau et vue grille', () => {
    cy.visit('/admin/sale');
    cy.wait('@getMyListings');

    cy.get('[data-cy="view-grid-btn"]').click();
    cy.get('[data-cy="sale-grid-container"]').should('be.visible');
    cy.get('[data-cy="admin-sale-grid-card"]').should('have.length', 2);

    cy.get('[data-cy="view-table-btn"]').click();
    cy.get('[data-cy="sale-table-container"]').should('be.visible');
  });

  it('permet de filtrer les annonces par état (Neuf / Occasion)', () => {
    cy.visit('/admin/sale');
    cy.wait('@getMyListings');

    cy.get('[data-cy="filter-condition"]').click();
    cy.get('mat-option').contains(/Occasion|USED/i).click();

    cy.get('[data-cy="listing-title"]').should('have.length', 1);
    cy.get('[data-cy="listing-title"]').first().should('contain.text', 'Dacia Duster');
  });

  it('ouvre la modale des statistiques de performance', () => {
    cy.visit('/admin/sale');
    cy.wait('@getMyListings');

    cy.get('[data-cy="actions-btn"]').first().click();
    cy.get('[data-cy="action-stats"]').click();

    cy.get('[data-cy="sale-stats-dialog"]').should('be.visible');
    cy.get('[data-cy="stat-views"]').should('be.visible');
    cy.get('[data-cy="stat-contacts"]').should('be.visible');
    cy.get('[data-cy="close-stats-btn"]').click();
    cy.get('[data-cy="sale-stats-dialog"]').should('not.exist');
  });

  it('navigue vers le formulaire de création lors du clic sur Nouvelle annonce', () => {
    cy.visit('/admin/sale');
    cy.wait('@getMyListings');

    cy.get('[data-cy="new-sale-btn"]').should('have.attr', 'href', '/admin/sale/new');
  });
});
