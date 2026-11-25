describe('Parcours de vente - Demande de visite (/sale/:id)', () => {
  const mockListing = {
    id: 31,
    vehicleId: 12,
    titre: 'Dacia Duster 1.5 dCi Prestige',
    description: 'SUV polyvalent et robuste.',
    prixVente: 185000,
    prixNego: true,
    etat: 'USED',
    kilometrage: 52000,
    numeroChassis: 'VF1HJD20260315001',
    nombreProprietaires: 1,
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

  const tomorrow = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/sale-listings/31', {
      statusCode: 200,
      body: mockListing,
    }).as('getSaleDetail');

    cy.intercept('GET', '/api/vehicles/12/photos', { statusCode: 200, body: [] });
    cy.intercept('GET', '/api/sale-listings/31/history', { statusCode: 200, body: [] });
    cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
      statusCode: 200,
      body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 },
    });
  });

  it('exige une authentification pour les visiteurs non connectés', () => {
    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();
    cy.get('[data-cy="sale-visit-modal"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-required"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-link"]').should('have.attr', 'href', '/login');
  });

  it('affiche le formulaire complet de visite pour un client connecté', () => {
    cy.loginAsClient();
    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();
    cy.get('[data-cy="sale-visit-modal"]').should('be.visible');
    cy.get('[data-cy="sale-visit-form"]').should('be.visible');
    cy.get('[data-cy="sale-visit-date"]').should('be.visible');
    cy.get('[data-cy="sale-visit-slot"]').should('be.visible');
    cy.get('[data-cy="sale-visit-location"]').should('be.visible');
    cy.get('[data-cy="sale-visit-type"]').should('be.visible');
  });

  it('valide les champs obligatoires du formulaire de visite', () => {
    cy.loginAsClient();
    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();
    cy.get('[data-cy="sale-visit-form"]').should('be.visible');

    // Cliquer directement sur envoyer sans remplir
    cy.get('[data-cy="sale-visit-submit"]').click();

    cy.get('mat-error').should('have.length.at.least', 3);
  });

  it('envoie avec succès une demande de visite simple', () => {
    cy.loginAsClient();

    const visitDate = tomorrow();
    cy.intercept('POST', '/api/sale-listings/31/visit-request', (req) => {
      expect(req.body.dateSouhaitee).to.equal(visitDate);
      expect(req.body.creneauHoraire).to.equal('10:00 - 12:00');
      expect(req.body.lieu).to.equal('LocaVite Agence Casablanca');
      expect(req.body.typeVisite).to.equal('VISITE_SIMPLE');

      req.reply({
        statusCode: 201,
        body: {
          id: 501,
          saleListingId: 31,
          dateSouhaitee: visitDate,
          creneauHoraire: '10:00 - 12:00',
          lieu: 'LocaVite Agence Casablanca',
          typeVisite: 'VISITE_SIMPLE',
          statut: 'PENDING',
        },
      });
    }).as('requestVisit');

    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();

    cy.get('[data-cy="sale-visit-date"]').type(visitDate);
    cy.get('[data-cy="sale-visit-slot"]').type('10:00 - 12:00');
    cy.get('[data-cy="sale-visit-location"]').type('LocaVite Agence Casablanca');

    cy.get('[data-cy="sale-visit-submit"]').click();
    cy.wait('@requestVisit');

    cy.get('[data-cy="sale-visit-success"]').should('be.visible');
  });

  it('permet de sélectionner un essai routier et de le soumettre', () => {
    cy.loginAsClient();

    const visitDate = tomorrow();
    cy.intercept('POST', '/api/sale-listings/31/visit-request', (req) => {
      expect(req.body.typeVisite).to.equal('ESSAI_ROUTIER');

      req.reply({
        statusCode: 201,
        body: {
          id: 502,
          saleListingId: 31,
          dateSouhaitee: visitDate,
          creneauHoraire: '15:00 - 17:00',
          lieu: 'Casablanca Anfa',
          typeVisite: 'ESSAI_ROUTIER',
          statut: 'PENDING',
        },
      });
    }).as('requestTestDrive');

    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();

    cy.get('[data-cy="sale-visit-date"]').type(visitDate);
    cy.get('[data-cy="sale-visit-slot"]').type('15:00 - 17:00');
    cy.get('[data-cy="sale-visit-location"]').type('Casablanca Anfa');

    // Sélectionner type de visite
    cy.get('[data-cy="sale-visit-type"]').click();
    cy.get('mat-option').contains(/essai/i).click();

    cy.get('[data-cy="sale-visit-submit"]').click();
    cy.wait('@requestTestDrive');

    cy.get('[data-cy="sale-visit-success"]').should('be.visible');
  });

  it('affiche un message d erreur en cas d échec réseau', () => {
    cy.loginAsClient();

    cy.intercept('POST', '/api/sale-listings/31/visit-request', {
      statusCode: 500,
      body: { error: 'Server Error' },
    }).as('requestVisitError');

    cy.visit('/sale/31');
    cy.wait('@getSaleDetail');

    cy.get('[data-cy="sale-visit-open"]').click();

    cy.get('[data-cy="sale-visit-date"]').type(tomorrow());
    cy.get('[data-cy="sale-visit-slot"]').type('10:00 - 12:00');
    cy.get('[data-cy="sale-visit-location"]').type('Agence');

    cy.get('[data-cy="sale-visit-submit"]').click();
    cy.wait('@requestVisitError');

    cy.get('.error-message').should('be.visible');
    cy.get('[data-cy="sale-visit-success"]').should('not.exist');
  });
});
