import { MOCK_BRANDS, MOCK_CATEGORIES } from '../support/sale-fixtures';

describe('Parcours de vente - Liste et Filtres (/sale)', () => {
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

    cy.fixture('sale-listings.json').then((listings) => {
      cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, (req) => {
        const etat = req.query['etat'];
        const marque = req.query['marque'];
        let filtered = [...listings];

        if (etat) {
          filtered = filtered.filter((l) => l.etat === etat);
        }
        if (marque) {
          filtered = filtered.filter((l) => l.marque.toLowerCase() === String(marque).toLowerCase());
        }

        req.reply({
          statusCode: 200,
          body: {
            content: filtered,
            totalElements: filtered.length,
            totalPages: Math.ceil(filtered.length / Number(req.query['size'] ?? 12)) || 1,
            number: Number(req.query['page'] ?? 0),
            size: Number(req.query['size'] ?? 12),
          },
        });
      }).as('searchSaleListings');
    });
  });

  it('affiche la liste des annonces de vente avec grille, cartes et prix', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');

    cy.get('[data-cy="sale-page"]').should('be.visible');
    cy.get('[data-cy="sale-results-count"]').should('be.visible').and('contain.text', '4');
    cy.get('[data-cy="sale-grid"]').should('be.visible');
    cy.get('[data-cy="sale-card"]').should('have.length', 4);

    // Vérifier la première carte (Dacia Duster - Occasion)
    cy.get('[data-cy="sale-card"]').first().within(() => {
      cy.get('[data-cy="sale-title"]').should('contain.text', 'Dacia Duster');
      cy.get('[data-cy="sale-condition"]').should('contain.text', 'OCCASION');
      cy.get('[data-cy="sale-price"]').should('contain.text', '185');
      cy.get('[data-cy="sale-details"]').should('have.attr', 'href', '/sale/31');
      cy.get('[data-cy="sale-favorite"]').should('have.attr', 'aria-pressed', 'false');
    });

    // Vérifier la deuxième carte (Peugeot 208 - Neuf)
    cy.get('[data-cy="sale-card"]').eq(1).within(() => {
      cy.get('[data-cy="sale-title"]').should('contain.text', 'Peugeot 208');
      cy.get('[data-cy="sale-condition"]').should('contain.text', 'NEUVE');
      cy.get('[data-cy="sale-price"]').should('contain.text', '220');
      cy.get('[data-cy="sale-details"]').should('have.attr', 'href', '/sale/32');
    });
  });

  it('filtre les annonces par état (Neuf / Occasion)', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');

    // Sélectionner Occasion
    cy.get('[data-cy="sale-filter-condition"]').click();
    cy.get('mat-option').contains(/Occasion|USED/i).click();

    cy.wait('@searchSaleListings').its('request.url').should('contain', 'etat=USED');
    cy.get('[data-cy="sale-card"]').should('have.length', 3);
    cy.get('[data-cy="sale-card"]').each(($card) => {
      cy.wrap($card).find('[data-cy="sale-condition"]').should('contain.text', 'OCCASION');
    });

    // Sélectionner Neuf
    cy.get('[data-cy="sale-filter-condition"]').click();
    cy.get('mat-option').contains(/NEUVE|Neuf|NEW/i).click();

    cy.wait('@searchSaleListings').its('request.url').should('contain', 'etat=NEW');
    cy.get('[data-cy="sale-card"]').should('have.length', 1);
    cy.get('[data-cy="sale-card"]').first().find('[data-cy="sale-condition"]').should('contain.text', 'NEUVE');
  });

  it('applique les filtres depuis les paramètres de l URL', () => {
    cy.visit('/sale?etat=USED&marque=Dacia&prixMin=50000&anneeMin=2020');
    cy.wait('@searchSaleListings').its('request.url').should((url) => {
      expect(url).to.contain('etat=USED');
      expect(url).to.contain('marque=Dacia');
      expect(url).to.contain('prixMin=50000');
      expect(url).to.contain('anneeMin=2020');
    });

    cy.get('[data-cy="sale-card"]').should('have.length', 1);
    cy.get('[data-cy="sale-card"]').first().find('[data-cy="sale-title"]').should('contain.text', 'Dacia Duster');
  });

  it('permet de changer le tri des annonces', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');

    cy.get('[data-cy="sale-sort"]').click();
    cy.get('mat-option').first().click(); // Prix croissant

    cy.wait('@searchSaleListings').its('request.url').should('contain', 'sort=prix,asc');
  });

  it('gère la pagination des résultats', () => {
    cy.fixture('sale-listings.json').then((listings) => {
      cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, (req) => {
        req.reply({
          statusCode: 200,
          body: {
            content: listings.slice(0, 2),
            totalElements: 20,
            totalPages: 10,
            number: Number(req.query['page'] ?? 0),
            size: 2,
          },
        });
      }).as('searchPagedListings');
    });

    cy.visit('/sale');
    cy.wait('@searchPagedListings');

    cy.get('[data-cy="sale-paginator"]').should('be.visible');
    cy.get('mat-paginator button.mat-mdc-paginator-navigation-next').click();

    cy.wait('@searchPagedListings').its('request.url').should('contain', 'page=1');
    cy.location('search').should('contain', 'page=1');
  });

  it('affiche l état vide lorsqu aucun résultat ne correspond', () => {
    cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 12,
      },
    }).as('searchEmptyListings');

    cy.visit('/sale');
    cy.wait('@searchEmptyListings');

    cy.get('[data-cy="sale-empty"]').should('be.visible');
    cy.get('[data-cy="sale-grid"]').should('not.exist');
  });

  it('permet d ajouter une annonce aux favoris', () => {
    cy.visit('/sale');
    cy.wait('@searchSaleListings');

    cy.get('[data-cy="sale-card"]').first().within(() => {
      cy.get('[data-cy="sale-favorite"]')
        .should('have.attr', 'aria-pressed', 'false')
        .click()
        .should('have.attr', 'aria-pressed', 'true');
    });
  });
});
