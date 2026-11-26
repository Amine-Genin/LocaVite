import { setupSaleDetailIntercepts, visitSaleDetail } from '../support/sale-fixtures';

describe('Parcours de vente - Demande de visite (/sale/:id)', () => {
  const getNextDayFormatted = () => {
    const nextDate = new Date();
    nextDate.setDate(nextDate.getDate() + 1);
    const yyyy = nextDate.getFullYear();
    const mm = String(nextDate.getMonth() + 1).padStart(2, '0');
    const dd = String(nextDate.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  beforeEach(() => {
    setupSaleDetailIntercepts();
  });

  const openVisitModal = () => {
    visitSaleDetail();
    cy.get('[data-cy="sale-visit-open"]').click();
  };

  it('exige une authentification pour les visiteurs non connectés', () => {
    openVisitModal();
    cy.get('[data-cy="sale-visit-modal"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-required"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-link"]').should('have.attr', 'href', '/login');
  });

  it('affiche le formulaire complet de visite pour un client connecté', () => {
    cy.loginAsClient();
    openVisitModal();
    cy.get('[data-cy="sale-visit-modal"]').should('be.visible');
    cy.get('[data-cy="sale-visit-form"]').should('be.visible');
    cy.get('[data-cy="sale-visit-date"]').should('be.visible');
    cy.get('[data-cy="sale-visit-slot"]').should('be.visible');
    cy.get('[data-cy="sale-visit-location"]').should('be.visible');
    cy.get('[data-cy="sale-visit-type"]').should('be.visible');
  });

  it('valide les champs obligatoires du formulaire de visite', () => {
    cy.loginAsClient();
    openVisitModal();
    cy.get('[data-cy="sale-visit-form"]').should('be.visible');

    // Cliquer directement sur envoyer sans remplir
    cy.get('[data-cy="sale-visit-submit"]').click();

    cy.get('mat-error').should('have.length.at.least', 3);
  });

  it('envoie avec succès une demande de visite simple', () => {
    cy.loginAsClient();

    const dateTarget = getNextDayFormatted();
    cy.intercept('POST', '/api/sale-listings/31/visit-request', (req) => {
      expect(req.body.dateSouhaitee).to.equal(dateTarget);
      expect(req.body.creneauHoraire).to.equal('10:00 - 12:00');
      expect(req.body.lieu).to.equal('LocaVite Agence Casablanca');
      expect(req.body.typeVisite).to.equal('VISITE_SIMPLE');

      req.reply({
        statusCode: 201,
        body: {
          id: 501,
          saleListingId: 31,
          dateSouhaitee: dateTarget,
          creneauHoraire: '10:00 - 12:00',
          lieu: 'LocaVite Agence Casablanca',
          typeVisite: 'VISITE_SIMPLE',
          statut: 'PENDING',
        },
      });
    }).as('requestVisit');

    openVisitModal();

    cy.get('[data-cy="sale-visit-date"]').type(dateTarget);
    cy.get('[data-cy="sale-visit-slot"]').type('10:00 - 12:00');
    cy.get('[data-cy="sale-visit-location"]').type('LocaVite Agence Casablanca');

    cy.get('[data-cy="sale-visit-submit"]').click();
    cy.wait('@requestVisit');

    cy.get('[data-cy="sale-visit-success"]').should('be.visible');
  });

  it('permet de sélectionner un essai routier et de le soumettre', () => {
    cy.loginAsClient();

    const dateTarget = getNextDayFormatted();
    cy.intercept('POST', '/api/sale-listings/31/visit-request', (req) => {
      expect(req.body.typeVisite).to.equal('ESSAI_ROUTIER');

      req.reply({
        statusCode: 201,
        body: {
          id: 502,
          saleListingId: 31,
          dateSouhaitee: dateTarget,
          creneauHoraire: '15:00 - 17:00',
          lieu: 'Casablanca Anfa',
          typeVisite: 'ESSAI_ROUTIER',
          statut: 'PENDING',
        },
      });
    }).as('requestTestDrive');

    openVisitModal();

    cy.get('[data-cy="sale-visit-date"]').type(dateTarget);
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

    openVisitModal();

    cy.get('[data-cy="sale-visit-date"]').type(getNextDayFormatted());
    cy.get('[data-cy="sale-visit-slot"]').type('10:00 - 12:00');
    cy.get('[data-cy="sale-visit-location"]').type('Agence');

    cy.get('[data-cy="sale-visit-submit"]').click();
    cy.wait('@requestVisitError');

    cy.get('.error-message').should('be.visible');
    cy.get('[data-cy="sale-visit-success"]').should('not.exist');
  });
});
