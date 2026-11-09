describe('Formulaire ajout véhicule (admin Angular) (/admin/vehicles/new)', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('redirection vers /login si non authentifié', () => {
    cy.visit('/admin/vehicles/new');
    cy.url().should('include', '/login');
  });

  it('redirection vers /login si rôle non autorisé (CLIENT)', () => {
    cy.login('client@locavite.ma', 'ClientPass123!', ['CLIENT']);
    cy.visit('/admin/vehicles/new');
    cy.url().should('include', '/login');
  });

  it('accès autorisé pour rôle AGENCE/ADMIN et affichage de toutes les sections', () => {
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands }).as('getBrands');
      cy.intercept('GET', '/api/categories', {
        statusCode: 200,
        body: data.categories,
      }).as('getCategories');

      cy.login('admin.dev@locavite.ma', 'AdminDev2026!', ['AGENCE', 'ADMIN']);
      cy.visit('/admin/vehicles/new');

      cy.get('[data-cy="page-title"]').should('be.visible');
      cy.get('[data-cy="section-identity"]').should('be.visible');
      cy.get('[data-cy="section-features"]').should('be.visible');
      cy.get('[data-cy="section-pricing"]').should('be.visible');
      cy.get('[data-cy="section-location"]').should('be.visible');
      cy.get('[data-cy="section-photos"]').should('be.visible');

      cy.get('[data-cy="publish-button"]').should('be.disabled');
      cy.get('[data-cy="draft-button"]').should('be.disabled');
    });
  });

  it('upload de 3 photos, prévisualisation, réorganisation et sélection photo principale', () => {
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands }).as('getBrands');
      cy.intercept('GET', '/api/categories', {
        statusCode: 200,
        body: data.categories,
      }).as('getCategories');

      cy.login('agency@locavite.ma', 'AdminDev2026!', ['AGENCE']);
      cy.visit('/admin/vehicles/new');

      const photoFiles = [
        {
          contents: Cypress.Buffer.from('fake-photo-1'),
          fileName: 'car1.jpg',
          mimeType: 'image/jpeg',
        },
        {
          contents: Cypress.Buffer.from('fake-photo-2'),
          fileName: 'car2.jpg',
          mimeType: 'image/jpeg',
        },
        {
          contents: Cypress.Buffer.from('fake-photo-3'),
          fileName: 'car3.jpg',
          mimeType: 'image/jpeg',
        },
      ];

      cy.get('[data-cy="file-input"]').selectFile(photoFiles, { force: true });

      cy.get('[data-cy="photo-preview"]').should('have.length', 3);
      cy.get('[data-cy="primary-badge"]').should('be.visible');

      cy.get('[data-cy="set-primary-btn"]').eq(1).click({ force: true });
      cy.get('[data-cy="photo-preview"]')
        .eq(1)
        .find('[data-cy="primary-badge"]')
        .should('be.visible');

      cy.get('[data-cy="remove-photo-btn"]').eq(2).click({ force: true });
      cy.get('[data-cy="photo-preview"]').should('have.length', 2);
    });
  });

  it('remplissage complet via cy.createVehicle(), publication et redirection vers /admin/vehicles', () => {
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands }).as('getBrands');
      cy.intercept('GET', '/api/categories', {
        statusCode: 200,
        body: data.categories,
      }).as('getCategories');

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
        req.reply({ statusCode: 201, body: data.newVehicle });
      }).as('createVehicleApi');

      cy.intercept('POST', '/api/vehicles/99/photos', {
        statusCode: 201,
        body: [{ id: 501, vehicleId: 99, url: '/photos/501.jpg', isPrimary: true, ordre: 0 }],
      }).as('uploadPhotos');

      cy.login('admin.dev@locavite.ma', 'AdminDev2026!', ['ADMIN']);
      cy.visit('/admin/vehicles/new');
      cy.wait(['@getBrands', '@getCategories']);

      cy.createVehicle({
        brandSearch: 'ren',
        marque: 'Renault',
        modele: 'Clio 5',
        annee: 2023,
        categorie: 'Économique',
        carburant: 'ESSENCE',
        transmission: 'MANUELLE',
        places: 5,
        matricule: '12345-A-1',
        description: 'Superbe citadine très bien entretenue',
        prixJour: 350,
        prixVente: 145000,
        ville: 'Casablanca',
        statut: 'PUBLISHED',
        withPhoto: true,
      });

      cy.wait('@createVehicleApi');
      cy.wait('@uploadPhotos');

      cy.url().should('include', '/admin/vehicles');
    });
  });

  it('sauvegarde en tant que brouillon (statut DRAFT) via cy.createVehicle()', () => {
    cy.fixture('vehicles.json').then((data) => {
      cy.intercept('GET', '/api/brands', { statusCode: 200, body: data.brands }).as('getBrands');
      cy.intercept('GET', '/api/categories', {
        statusCode: 200,
        body: data.categories,
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

      cy.login('agency@locavite.ma', 'AdminDev2026!', ['AGENCE']);
      cy.visit('/admin/vehicles/new');
      cy.wait(['@getBrands', '@getCategories']);

      cy.createVehicle({
        brandSearch: 'dac',
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2024,
        categorie: 'SUV',
        carburant: 'DIESEL',
        transmission: 'MANUELLE',
        places: 5,
        matricule: '99999-B-1',
        description: 'Brouillon en cours de saisie',
        prixJour: 400,
        prixVente: 180000,
        ville: 'Casablanca',
        statut: 'DRAFT',
        withPhoto: false,
      });

      cy.wait('@createDraftVehicle');
      cy.url().should('include', '/admin/vehicles');
    });
  });
});
