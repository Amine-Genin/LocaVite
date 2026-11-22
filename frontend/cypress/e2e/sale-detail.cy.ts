const listing = {
  id: 31,
  vehicleId: 12,
  titre: 'Dacia Duster',
  description: 'SUV polyvalent et robuste.',
  prixVente: 185000,
  prixNego: true,
  etat: 'USED',
  kilometrage: 52000,
  numeroChassis: 'VIN-123',
  nombreProprietaires: 1,
  marque: 'Dacia',
  modele: 'Duster',
  annee: 2022,
  ville: 'Casablanca',
  carburant: 'DIESEL',
  transmission: 'MANUELLE',
  categorie: 'SUV',
  photoUrl: 'https://example.com/duster.jpg',
  publishedAt: '2026-03-01T10:00:00Z',
};

function interceptSaleDetail() {
  cy.intercept('GET', '/api/sale-listings/31', { statusCode: 200, body: listing });
  cy.intercept('GET', '/api/vehicles/12/photos', { statusCode: 200, body: [] });
  cy.intercept('GET', '/api/sale-listings/31/history', { statusCode: 200, body: [] });
  cy.intercept('GET', '/api/sale-listings/search*', {
    statusCode: 200,
    body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 },
  });
}

describe('Sale listing detail (/sale/:id)', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    interceptSaleDetail();
  });

  it('displays the listing details and opens the contact form', () => {
    cy.visit('/sale/31');
    cy.get('[data-cy="sale-detail-title"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-cy="sale-detail-year"]').should('contain.text', '2022');
    cy.get('[data-cy="sale-detail-vin"]').should('contain.text', 'VIN-123');
    cy.get('[data-cy="sale-detail-price"]').should('contain.text', '185');
    cy.get('[data-cy="sale-contact-open"]').click();
    cy.get('[data-cy="sale-contact-modal"]').should('be.visible');
    cy.get('[data-cy="sale-contact-form"]').should('be.visible');
  });

  it('asks visitors to sign in before requesting a visit', () => {
    cy.visit('/sale/31');
    cy.get('[data-cy="sale-visit-open"]').click();
    cy.get('[data-cy="sale-visit-modal"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-required"]').should('be.visible');
    cy.get('[data-cy="sale-visit-login-link"]').should('have.attr', 'href', '/login');
  });
});
