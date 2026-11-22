const listings = [
  {
    id: 31,
    vehicleId: 12,
    titre: 'Dacia Duster',
    prixVente: 185000,
    etat: 'USED',
    kilometrage: 52000,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2022,
    ville: 'Casablanca',
    carburant: 'DIESEL',
    categorie: 'SUV',
    photoUrl: 'https://example.com/duster.jpg',
    publishedAt: '2026-03-01T10:00:00Z',
  },
];

function interceptSaleListings() {
  cy.intercept('GET', '/api/brands', { statusCode: 200, body: [{ id: 1, name: 'Dacia' }] });
  cy.intercept('GET', '/api/categories', { statusCode: 200, body: [{ id: 1, name: 'SUV' }] });
  cy.intercept('GET', '/api/sale-listings/search*', (request) => {
    request.reply({
      statusCode: 200,
      body: {
        content: listings,
        totalElements: 25,
        totalPages: 2,
        number: Number(request.query['page'] ?? 0),
        size: Number(request.query['size'] ?? 12),
      },
    });
  }).as('searchSaleListings');
}

describe('Sale listings (/sale)', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    interceptSaleListings();
  });

  it('displays sale listing cards with vehicle details and price', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');

    cy.get('[data-cy="sale-grid"]').should('be.visible');
    cy.get('[data-cy="sale-card"]')
      .should('have.length', 1)
      .within(() => {
        cy.get('[data-cy="sale-title"]').should('contain.text', 'Dacia Duster');
        cy.get('[data-cy="sale-condition"]').should('contain.text', 'OCCASION');
        cy.get('[data-cy="sale-price"]').should('contain.text', '185');
        cy.get('[data-cy="sale-favorite"]').should('have.attr', 'aria-pressed', 'false');
      });
    cy.get('[data-cy="sale-results-count"]').should('contain.text', '25');
  });

  it('applies filters from the URL and sends them to the public search endpoint', () => {
    cy.visit('/sale?etat=USED&marque=Dacia&prixMin=50000&anneeMin=2020');
    cy.wait('@searchSaleListings')
      .its('request.url')
      .should((url) => {
        expect(url).to.contain('etat=USED');
        expect(url).to.contain('marque=Dacia');
        expect(url).to.contain('prixMin=50000');
        expect(url).to.contain('anneeMin=2020');
      });
    cy.get('[data-cy="sale-filter-condition"]').should('contain.text', 'OCCASION');
  });

  it('updates the URL and search when changing pages', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');
    cy.get('mat-paginator button.mat-mdc-paginator-navigation-next').click();
    cy.wait('@searchSaleListings').its('request.url').should('contain', 'page=1');
    cy.location('search').should('contain', 'page=1');
  });
});
