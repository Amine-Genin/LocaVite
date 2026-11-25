describe('Parcours de vente - Estimation de prix ML (/admin/sale/new et /sale/:id)', () => {
  const mockVehicles = [
    {
      id: 10,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2022,
      ville: 'Casablanca',
      categorie: 'SUV',
      prixJour: 350,
      prixVente: 180000,
      matricule: '78945-A-1',
      statut: 'PUBLISHED',
      agenceId: 3,
    },
  ];

  const mockUsedListing = {
    id: 31,
    vehicleId: 10,
    titre: 'Dacia Duster 1.5 dCi Prestige',
    description: 'SUV polyvalent.',
    prixVente: 185000,
    prixNego: true,
    etat: 'USED',
    kilometrage: 52000,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2022,
    ville: 'Casablanca',
    carburant: 'DIESEL',
    transmission: 'MANUELLE',
    categorie: 'SUV',
    photoUrl: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d',
    publishedAt: '2026-03-01T10:00:00Z',
  };

  const mockNewListing = {
    ...mockUsedListing,
    id: 32,
    titre: 'Peugeot 208 GT Neuve',
    etat: 'NEW',
    kilometrage: 0,
    marque: 'Peugeot',
    modele: '208',
  };

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();
  });

  describe('Estimation dans le formulaire de vente agence (/admin/sale/new)', () => {
    beforeEach(() => {
      cy.loginAsAgence();

      cy.intercept('GET', '/api/brands', { statusCode: 200, body: [{ id: 1, name: 'Dacia' }] });
      cy.intercept('GET', '/api/categories', { statusCode: 200, body: [{ id: 1, name: 'SUV' }] });
      cy.intercept('GET', /\/api\/vehicles(\/search)?(\?.*)?$/, {
        statusCode: 200,
        body: { content: mockVehicles, totalElements: 1, totalPages: 1, number: 0, size: 10 },
      }).as('getVehicles');
    });

    it('calcule et affiche l estimation ML indicative avec fourchette de prix', () => {
      cy.intercept('POST', '/api/sale-listings/estimate', (req) => {
        expect(req.body.marque).to.equal('Dacia');
        expect(req.body.modele).to.equal('Duster');
        expect(req.body.etat).to.equal('USED');

        req.reply({
          statusCode: 200,
          body: {
            estimatedPrice: 182000,
            minimumPrice: 165000,
            maximumPrice: 198000,
            comparableListings: 14,
          },
        });
      }).as('estimateML');

      cy.visit('/admin/sale/new');
      cy.wait('@getVehicles');

      // Étape 1 : Sélection véhicule existant
      cy.get('[data-cy="existing-vehicle-select"]').click();
      cy.get('mat-option').first().click();
      cy.get('[data-cy="step-vehicle-next"]').click();

      // Étape 2 : État
      cy.get('[data-cy="step-condition-next"]').click();

      // Étape 3 : Estimation de prix
      cy.get('[data-cy="estimate-price-button"]').should('be.visible').click();
      cy.wait('@estimateML');

      cy.get('[data-cy="sale-estimate-result"]').should('be.visible').within(() => {
        cy.contains('182').should('be.visible');
        cy.contains('165').should('be.visible');
        cy.contains('198').should('be.visible');
        cy.contains('14').should('be.visible');
      });
    });

    it('affiche un message lorsqu aucune annonce comparable n est disponible', () => {
      cy.intercept('POST', '/api/sale-listings/estimate', {
        statusCode: 200,
        body: {
          estimatedPrice: null,
          minimumPrice: null,
          maximumPrice: null,
          comparableListings: 0,
        },
      }).as('estimateEmpty');

      cy.visit('/admin/sale/new');
      cy.wait('@getVehicles');

      cy.get('[data-cy="existing-vehicle-select"]').click();
      cy.get('mat-option').first().click();
      cy.get('[data-cy="step-vehicle-next"]').click();
      cy.get('[data-cy="step-condition-next"]').click();

      cy.get('[data-cy="estimate-price-button"]').click();
      cy.wait('@estimateEmpty');

      cy.get('[data-cy="sale-estimate-result"]').should('contain.text', 'Aucune annonce comparable disponible');
    });

    it('affiche une bannière d erreur si le service d estimation ML échoue', () => {
      cy.intercept('POST', '/api/sale-listings/estimate', {
        statusCode: 500,
        body: { error: 'ML Engine unavailable' },
      }).as('estimateError');

      cy.visit('/admin/sale/new');
      cy.wait('@getVehicles');

      cy.get('[data-cy="existing-vehicle-select"]').click();
      cy.get('mat-option').first().click();
      cy.get('[data-cy="step-vehicle-next"]').click();
      cy.get('[data-cy="step-condition-next"]').click();

      cy.get('[data-cy="estimate-price-button"]').click();
      cy.wait('@estimateError');

      cy.get('[data-cy="sale-form-error"]').should('be.visible');
    });
  });

  describe('Estimation sur la page de détail (/sale/:id)', () => {
    it('affiche l estimation basée sur le marché pour un véhicule d occasion', () => {
      cy.intercept('GET', '/api/sale-listings/31', { statusCode: 200, body: mockUsedListing });
      cy.intercept('GET', '/api/vehicles/10/photos', { statusCode: 200, body: [] });
      cy.intercept('GET', '/api/sale-listings/31/history', { statusCode: 200, body: [] });

      cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
        statusCode: 200,
        body: {
          content: [
            { id: 91, vehicleId: 81, prixVente: 178000 },
            { id: 92, vehicleId: 82, prixVente: 184000 },
            { id: 93, vehicleId: 83, prixVente: 190000 },
          ],
          totalElements: 3,
          totalPages: 1,
          number: 0,
          size: 100,
        },
      }).as('getComparableListings');

      cy.visit('/sale/31');
      cy.wait('@getComparableListings');

      cy.get('[data-cy="sale-estimate-toggle"]').should('be.visible').click();
      cy.get('[data-cy="sale-estimate-result"]')
        .scrollIntoView()
        .should('be.visible')
        .and('contain.text', '184')
        .and('contain.text', '3');
    });
    });

    it('masque le bouton d estimation pour un véhicule neuf', () => {
      cy.intercept('GET', '/api/sale-listings/32', { statusCode: 200, body: mockNewListing });
      cy.intercept('GET', '/api/vehicles/10/photos', { statusCode: 200, body: [] });
      cy.intercept('GET', '/api/sale-listings/32/history', { statusCode: 200, body: [] });
      cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
        statusCode: 200,
        body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 },
      });

      cy.visit('/sale/32');

      cy.get('[data-cy="sale-detail-title"]').should('contain.text', 'Peugeot 208');
      cy.get('[data-cy="sale-estimate-toggle"]').should('not.exist');
    });
  });
});
