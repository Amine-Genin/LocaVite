import { MOCK_AGENCY_VEHICLES, MOCK_BRANDS, MOCK_CATEGORIES } from '../support/sale-fixtures';

describe('Parcours de vente - Création et publication d annonce (/admin/sale/new)', () => {
  const mockVehicles = MOCK_AGENCY_VEHICLES;

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/brands', {
      statusCode: 200,
      body: MOCK_BRANDS,
    }).as('getBrands');

    cy.intercept('GET', '/api/categories', {
      statusCode: 200,
      body: MOCK_CATEGORIES,
    }).as('getCategories');

    cy.intercept('GET', /\/api\/vehicles(\/search)?(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: mockVehicles,
        totalElements: mockVehicles.length,
        totalPages: 1,
        number: 0,
        size: 100,
      },
    }).as('getAgencyVehicles');
  });

  it('redirige vers la page de connexion si l utilisateur n est pas authentifié', () => {
    cy.visit('/admin/sale/new');
    cy.location('pathname').should('eq', '/login');
  });

  it('refuse l accès aux utilisateurs ayant uniquement le rôle CLIENT', () => {
    cy.loginAsClient();
    cy.visit('/admin/sale/new');
    cy.location('pathname').should('not.eq', '/admin/sale/new');
  });

  it('permet à une agence de créer et publier une annonce pour un véhicule existant', () => {
    cy.loginAsAgence();

    cy.intercept('POST', '/api/sale-listings', (req) => {
      expect(req.body.vehicleId).to.equal(10);
      expect(req.body.titre).to.equal('Dacia Duster 1.5 dCi Prestige');
      expect(req.body.prixVente).to.equal(185000);
      expect(req.body.etat).to.equal('USED');
      expect(req.body.statut).to.equal('PUBLISHED');
      expect(req.body.numeroChassis).to.equal('VF1HJD20260315001');

      req.reply({
        statusCode: 201,
        headers: { Location: '/api/sale-listings/31' },
        body: {
          id: 31,
          code: 'SL-ABC123',
          vehicleId: 10,
          titre: req.body.titre,
          prixVente: req.body.prixVente,
          statut: 'PUBLISHED',
          createdAt: new Date().toISOString(),
        },
      });
    }).as('createSaleListing');

    cy.intercept('GET', /\/api\/sale-listings(\?.*)?$/, {
      statusCode: 200,
      body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 },
    }).as('getAdminSaleListings');

    cy.visit('/admin/sale/new');
    cy.wait('@getAgencyVehicles');

    cy.get('[data-cy="sale-form-page"]').should('be.visible');

    // Utilisation de la commande personnalisée
    cy.createSaleListing({
      mode: 'EXISTING',
      vehicleIndex: 0,
      etat: 'USED',
      prixVente: 185000,
      prixNego: true,
      numeroChassis: 'VF1HJD20260315001',
      kilometrage: 45000,
      nombreProprietaires: 1,
      titre: 'Dacia Duster 1.5 dCi Prestige',
      description: 'Véhicule en parfait état, première main, révisé régulièrement.',
      statut: 'PUBLISHED',
    });

    cy.wait('@createSaleListing');
    cy.location('pathname').should('eq', '/admin/sale');
  });

  it('permet de sauvegarder une annonce sous forme de brouillon (DRAFT)', () => {
    cy.loginAsAgence();

    cy.intercept('POST', '/api/sale-listings', (req) => {
      expect(req.body.statut).to.equal('DRAFT');
      req.reply({
        statusCode: 201,
        headers: { Location: '/api/sale-listings/32' },
        body: {
          id: 32,
          code: 'SL-DRF999',
          vehicleId: 10,
          titre: req.body.titre,
          statut: 'DRAFT',
        },
      });
    }).as('saveDraft');

    cy.intercept('GET', /\/api\/sale-listings(\?.*)?$/, {
      statusCode: 200,
      body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 },
    });

    cy.visit('/admin/sale/new');
    cy.wait('@getAgencyVehicles');

    cy.createSaleListing({
      mode: 'EXISTING',
      vehicleIndex: 0,
      etat: 'USED',
      prixVente: 175000,
      prixNego: false,
      titre: 'Brouillon Duster 2022',
      description: 'En cours de préparation...',
      statut: 'DRAFT',
    });

    cy.wait('@saveDraft');
    cy.location('pathname').should('eq', '/admin/sale');
  });

  it('permet de visualiser l aperçu de l annonce avant publication', () => {
    cy.loginAsAgence();
    cy.visit('/admin/sale/new');
    cy.wait('@getAgencyVehicles');

    // Étape 1 : Véhicule
    cy.get('[data-cy="existing-vehicle-select"]').click();
    cy.get('mat-option').first().click();
    cy.get('[data-cy="step-vehicle-next"]').click();

    // Étape 2 : État
    cy.get('[data-cy="step-condition-next"]').click();

    // Étape 3 : Prix
    cy.get('[data-cy="sale-price-input"]').type('199000', { force: true });
    cy.get('[data-cy="step-price-next"]').click();

    // Étape 4 : Détails
    cy.get('[data-cy="sale-circulation-input"]').type('2023-01-15', { force: true });
    cy.get('[data-cy="sale-mileage-input"]').type('45000', { force: true });
    cy.get('[data-cy="step-details-next"]').click();

    // Étape 5 : Description
    cy.get('[data-cy="sale-title-input"]').type('Duster Haute Finition', { force: true });
    cy.get('[data-cy="sale-description-input"]').type('Description détaillée pour l aperçu.', { force: true });
    cy.get('[data-cy="step-description-next"]').click();

    // Étape 6 : Photos
    cy.get('[data-cy="step-photos-next"]').click();

    // Étape 7 : Aperçu
    cy.get('[data-cy="sale-preview-toggle"]').click();
    cy.get('[data-cy="sale-preview"]').should('be.visible').within(() => {
      cy.contains('Duster Haute Finition').should('be.visible');
      cy.contains('Description détaillée pour l aperçu.').should('be.visible');
      cy.contains('199').should('be.visible');
    });
  });
});
