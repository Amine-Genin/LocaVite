describe('Formulaire ajout véhicule (admin Angular) (/admin/vehicles/new)', () => {
  const createMockToken = (roles = ['AGENCE']) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(
      JSON.stringify({
        sub: 'agency@locavite.ma',
        roles,
        exp: Math.floor(Date.now() / 1000) + 3600,
      }),
    );
    return `${header}.${payload}.fakesignature`;
  };

  const mockBrands = [
    { id: 1, name: 'Renault' },
    { id: 2, name: 'Dacia' },
    { id: 3, name: 'Peugeot' },
  ];

  const mockCategories = [
    { id: 1, name: 'Économique' },
    { id: 2, name: 'SUV' },
    { id: 3, name: 'Berline' },
  ];

  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('redirection vers /login si non authentifié', () => {
    cy.visit('/admin/vehicles/new');
    cy.url().should('include', '/login');
  });

  it('redirection vers /login si rôle non autorisé (CLIENT)', () => {
    const clientUser = {
      id: 2,
      nom: 'Client',
      prenom: 'Test',
      email: 'client@locavite.ma',
      roles: ['CLIENT'],
    };
    const clientToken = createMockToken(['CLIENT']);

    cy.visit('/login', {
      onBeforeLoad(win) {
        win.localStorage.setItem('accessToken', clientToken);
        win.localStorage.setItem('currentUser', JSON.stringify(clientUser));
      },
    });

    cy.visit('/admin/vehicles/new');
    cy.url().should('include', '/login');
  });

  it('accès autorisé pour rôle AGENCE et affichage de toutes les sections', () => {
    const agencyUser = {
      id: 1,
      nom: 'Agence',
      prenom: 'Casablanca',
      email: 'agency@locavite.ma',
      roles: ['AGENCE'],
    };
    const agencyToken = createMockToken(['AGENCE']);

    cy.intercept('GET', '/api/brands', { statusCode: 200, body: mockBrands }).as('getBrands');
    cy.intercept('GET', '/api/categories', {
      statusCode: 200,
      body: mockCategories,
    }).as('getCategories');

    cy.visit('/admin/vehicles/new', {
      onBeforeLoad(win) {
        win.localStorage.setItem('accessToken', agencyToken);
        win.localStorage.setItem('currentUser', JSON.stringify(agencyUser));
      },
    });

    cy.get('[data-cy="page-title"]').should('be.visible');
    cy.get('[data-cy="section-identity"]').should('be.visible');
    cy.get('[data-cy="section-features"]').should('be.visible');
    cy.get('[data-cy="section-pricing"]').should('be.visible');
    cy.get('[data-cy="section-location"]').should('be.visible');
    cy.get('[data-cy="section-photos"]').should('be.visible');

    // Vérifier que les boutons de soumission sont désactivés au départ
    cy.get('[data-cy="publish-button"]').should('be.disabled');
    cy.get('[data-cy="draft-button"]').should('be.disabled');
  });

  it('upload de 3 photos, prévisualisation, réorganisation et sélection photo principale', () => {
    const agencyUser = {
      id: 1,
      nom: 'Agence',
      prenom: 'Casablanca',
      email: 'agency@locavite.ma',
      roles: ['AGENCE'],
    };
    const agencyToken = createMockToken(['AGENCE']);

    cy.intercept('GET', '/api/brands', { statusCode: 200, body: mockBrands }).as('getBrands');
    cy.intercept('GET', '/api/categories', {
      statusCode: 200,
      body: mockCategories,
    }).as('getCategories');

    cy.visit('/admin/vehicles/new', {
      onBeforeLoad(win) {
        win.localStorage.setItem('accessToken', agencyToken);
        win.localStorage.setItem('currentUser', JSON.stringify(agencyUser));
      },
    });

    // Upload de 3 fichiers images
    const photoFiles = [
      { contents: 'fake-photo-1', fileName: 'car1.jpg', mimeType: 'image/jpeg' },
      { contents: 'fake-photo-2', fileName: 'car2.jpg', mimeType: 'image/jpeg' },
      { contents: 'fake-photo-3', fileName: 'car3.jpg', mimeType: 'image/jpeg' },
    ];

    cy.get('[data-cy="file-input"]').selectFile(photoFiles, { force: true });

    // Vérifier que 3 cartes de preview s'affichent
    cy.get('[data-cy="photo-preview"]').should('have.length', 3);

    // La première photo doit avoir le badge principale
    cy.get('[data-cy="primary-badge"]').should('be.visible');

    // Cliquer sur le bouton étoile de la 2ème photo pour en faire la photo principale
    cy.get('[data-cy="set-primary-btn"]').eq(1).click({ force: true });
    cy.get('[data-cy="photo-preview"]')
      .eq(1)
      .find('[data-cy="primary-badge"]')
      .should('be.visible');

    // Supprimer la dernière photo
    cy.get('[data-cy="remove-photo-btn"]').eq(2).click({ force: true });
    cy.get('[data-cy="photo-preview"]').should('have.length', 2);
  });

  it('remplissage complet, publication et redirection vers /admin/vehicles', () => {
    const agencyUser = {
      id: 1,
      nom: 'Agence',
      prenom: 'Casablanca',
      email: 'agency@locavite.ma',
      roles: ['AGENCE'],
    };
    const agencyToken = createMockToken(['AGENCE']);

    cy.intercept('GET', '/api/brands', { statusCode: 200, body: mockBrands }).as('getBrands');
    cy.intercept('GET', '/api/categories', {
      statusCode: 200,
      body: mockCategories,
    }).as('getCategories');

    const createdVehicleResponse = {
      id: 99,
      marque: 'Renault',
      modele: 'Clio 5',
      annee: 2023,
      categorie: 'Économique',
      ville: 'Casablanca',
      prixJour: 350,
      prixVente: 145000,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      description: 'Superbe citadine très bien entretenue',
      matricule: '12345-A-1',
      statut: 'PUBLISHED',
      agenceId: 1,
      photos: [],
    };

    cy.intercept('POST', '/api/vehicles', (req) => {
      expect(req.body.modele).to.eq('Clio 5');
      expect(req.body.brandId).to.eq(1);
      expect(req.body.annee).to.eq(2023);
      expect(req.body.categoryId).to.eq(1);
      expect(req.body.prixJour).to.eq(350);
      expect(req.body.prixVente).to.eq(145000);
      expect(req.body.carburant).to.eq('ESSENCE');
      expect(req.body.transmission).to.eq('MANUELLE');
      expect(req.body.places).to.eq(5);
      expect(req.body.matricule).to.eq('12345-A-1');
      expect(req.body.statut).to.eq('PUBLISHED');
      req.reply({ statusCode: 201, body: createdVehicleResponse });
    }).as('createVehicle');

    cy.intercept('POST', '/api/vehicles/99/photos', {
      statusCode: 201,
      body: [{ id: 501, vehicleId: 99, url: '/photos/501.jpg', isPrimary: true, ordre: 0 }],
    }).as('uploadPhotos');

    cy.visit('/admin/vehicles/new', {
      onBeforeLoad(win) {
        win.localStorage.setItem('accessToken', agencyToken);
        win.localStorage.setItem('currentUser', JSON.stringify(agencyUser));
      },
    });

    // 1. Marque Autocomplete
    cy.get('[data-cy="brand-input"]').type('ren');
    cy.get('[data-cy="brand-option"]').contains('Renault').click();

    // 2. Modèle
    cy.get('[data-cy="modele-input"]').type('Clio 5');

    // 3. Année
    cy.get('[data-cy="annee-select"]').click();
    cy.get('[data-cy="annee-option"]').contains('2023').click();

    // 4. Catégorie
    cy.get('[data-cy="category-select"]').click();
    cy.get('[data-cy="category-option"]').contains('Économique').click();

    // 5. Carburant et Transmission (radio)
    cy.get('[data-cy="carburant-radio-essence"]').click();
    cy.get('[data-cy="transmission-radio-manuelle"]').click();

    // 6. Places
    cy.get('[data-cy="places-input"]').clear().type('5');

    // 7. Matricule
    cy.get('[data-cy="matricule-input"]').type('12345-A-1');

    // 8. Description
    cy.get('[data-cy="description-input"]').type('Superbe citadine très bien entretenue');

    // 9. Prix / jour & Prix de vente
    cy.get('[data-cy="prix-jour-input"]').type('350');
    cy.get('[data-cy="prix-vente-input"]').type('145000');

    // 10. Localisation (Ville)
    cy.get('[data-cy="ville-select"]').click();
    cy.get('[data-cy="ville-option"]').contains('Casablanca').click();

    // 11. Photos
    cy.get('[data-cy="file-input"]').selectFile(
      { contents: 'photo-data', fileName: 'clio-front.jpg', mimeType: 'image/jpeg' },
      { force: true },
    );
    cy.get('[data-cy="photo-preview"]').should('have.length', 1);

    // Les boutons doivent être activés
    cy.get('[data-cy="publish-button"]').should('not.be.disabled').click();

    cy.wait('@createVehicle');
    cy.wait('@uploadPhotos');

    // Redirection vers /admin/vehicles
    cy.url().should('include', '/admin/vehicles');
  });

  it('sauvegarde en tant que brouillon (statut DRAFT)', () => {
    const agencyUser = {
      id: 1,
      nom: 'Agence',
      prenom: 'Casablanca',
      email: 'agency@locavite.ma',
      roles: ['AGENCE'],
    };
    const agencyToken = createMockToken(['AGENCE']);

    cy.intercept('GET', '/api/brands', { statusCode: 200, body: mockBrands }).as('getBrands');
    cy.intercept('GET', '/api/categories', {
      statusCode: 200,
      body: mockCategories,
    }).as('getCategories');

    cy.intercept('POST', '/api/vehicles', (req) => {
      expect(req.body.statut).to.eq('DRAFT');
      req.reply({
        statusCode: 201,
        body: {
          id: 100,
          ...req.body,
          marque: 'Dacia',
          categorie: 'SUV',
        },
      });
    }).as('createDraftVehicle');

    cy.visit('/admin/vehicles/new', {
      onBeforeLoad(win) {
        win.localStorage.setItem('accessToken', agencyToken);
        win.localStorage.setItem('currentUser', JSON.stringify(agencyUser));
      },
    });

    cy.get('[data-cy="brand-input"]').type('dac');
    cy.get('[data-cy="brand-option"]').contains('Dacia').click();
    cy.get('[data-cy="modele-input"]').type('Duster');
    cy.get('[data-cy="annee-select"]').click();
    cy.get('[data-cy="annee-option"]').contains('2024').click();
    cy.get('[data-cy="category-select"]').click();
    cy.get('[data-cy="category-option"]').contains('SUV').click();
    cy.get('[data-cy="places-input"]').clear().type('5');
    cy.get('[data-cy="matricule-input"]').type('99999-B-1');
    cy.get('[data-cy="description-input"]').type('Brouillon en cours de saisie');
    cy.get('[data-cy="prix-jour-input"]').type('400');
    cy.get('[data-cy="prix-vente-input"]').type('180000');

    cy.get('[data-cy="draft-button"]').should('not.be.disabled').click();

    cy.wait('@createDraftVehicle');
    cy.url().should('include', '/admin/vehicles');
  });
});
