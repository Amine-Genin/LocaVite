import { setupSaleDetailIntercepts, visitSaleDetail } from '../support/sale-fixtures';

describe('Parcours de vente - Formulaire de contact (/sale/:id)', () => {
  beforeEach(() => {
    setupSaleDetailIntercepts();
  });

  const openContactModal = () => {
    visitSaleDetail();
    cy.get('[data-cy="sale-contact-open"]').click();
  };

  it('ouvre la modale de contact depuis la fiche annonce', () => {
    openContactModal();
    cy.get('[data-cy="sale-contact-modal"]').should('be.visible');
    cy.get('[data-cy="sale-contact-form"]').should('be.visible');
    cy.get('[data-cy="sale-contact-name"]').should('be.visible');
    cy.get('[data-cy="sale-contact-email"]').should('be.visible');
    cy.get('[data-cy="sale-contact-phone"]').should('be.visible');
    cy.get('[data-cy="sale-contact-message"]').should('be.visible');
  });

  it('valide les champs obligatoires du formulaire de contact', () => {
    openContactModal();

    // Vider le message par défaut
    cy.get('[data-cy="sale-contact-message"]').clear();
    cy.get('[data-cy="sale-contact-submit"]').click();

    // Vérifier les erreurs de validation
    cy.get('mat-error').should('have.length.at.least', 3);
  });

  it('valide le format invalide d adresse e-mail', () => {
    openContactModal();

    cy.get('[data-cy="sale-contact-email"]').type('adresse-invalide');
    cy.get('[data-cy="sale-contact-name"]').click(); // Blur

    cy.get('mat-error').should('be.visible');
  });

  it('envoie avec succès une demande de contact en tant que visiteur', () => {
    cy.intercept('POST', '/api/sale-listings/31/contact', (req) => {
      expect(req.body.nom).to.equal('Karim Tazi');
      expect(req.body.email).to.equal('karim.tazi@example.com');
      expect(req.body.telephone).to.equal('0661998877');
      expect(req.body.message).to.contain('Duster');

      req.reply({
        statusCode: 200,
        body: { id: 10, saleListingId: 31 },
      });
    }).as('sendContact');

    openContactModal();

    cy.get('[data-cy="sale-contact-name"]').type('Karim Tazi');
    cy.get('[data-cy="sale-contact-email"]').type('karim.tazi@example.com');
    cy.get('[data-cy="sale-contact-phone"]').type('0661998877');

    cy.get('[data-cy="sale-contact-submit"]').click();
    cy.wait('@sendContact');

    cy.get('[data-cy="sale-contact-success"]').should('be.visible');
  });

  it('pré-remplit automatiquement les coordonnées de l utilisateur connecté', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma', 'ClientPass123!', {
      prenom: 'Ahmed',
      nom: 'Benali',
      telephone: '0612345678',
    });

    openContactModal();

    cy.get('[data-cy="sale-contact-name"]').should('have.value', 'Ahmed Benali');
    cy.get('[data-cy="sale-contact-email"]').should('have.value', 'ahmed.benali@locavite.ma');
    cy.get('[data-cy="sale-contact-phone"]').should('have.value', '0612345678');
  });

  it('affiche un message d erreur en cas d échec de l API', () => {
    cy.intercept('POST', '/api/sale-listings/31/contact', {
      statusCode: 500,
      body: { error: 'Internal Server Error' },
    }).as('sendContactFail');

    openContactModal();

    cy.get('[data-cy="sale-contact-name"]').type('Karim Tazi');
    cy.get('[data-cy="sale-contact-email"]').type('karim.tazi@example.com');
    cy.get('[data-cy="sale-contact-phone"]').type('0661998877');

    cy.get('[data-cy="sale-contact-submit"]').click();
    cy.wait('@sendContactFail');

    cy.get('.error-message').should('exist');
    cy.get('[data-cy="sale-contact-success"]').should('not.exist');
  });
});
