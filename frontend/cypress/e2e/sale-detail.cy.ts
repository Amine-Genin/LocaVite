import { MOCK_SALE_LISTING, visitSaleDetail } from '../support/sale-fixtures';

describe('Parcours de vente - Détail, Galerie et Historique (/sale/:id)', () => {
  const mockListing = MOCK_SALE_LISTING;

  const mockPhotos = [
    { id: 101, vehicleId: 12, url: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d', isPrimary: true },
    { id: 102, vehicleId: 12, url: 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341', isPrimary: false },
    { id: 103, vehicleId: 12, url: 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d', isPrimary: false },
  ];

  const mockHistory = [
    {
      id: 201,
      typeEvenement: 'ENTRETIEN',
      date: '2023-03-20',
      description: 'Première révision annuelle concessionnaire (15 000 km)',
      kilometrage: 15200,
      verified: true,
      documentUrl: 'https://example.com/facture1.pdf',
    },
    {
      id: 202,
      typeEvenement: 'CONTROLE_TECHNIQUE',
      date: '2024-03-18',
      description: 'Contrôle périodique vierge',
      kilometrage: 32000,
      verified: true,
      documentUrl: null,
    },
    {
      id: 203,
      typeEvenement: 'REPARATION',
      date: '2025-06-10',
      description: 'Remplacement plaquettes de frein avant',
      kilometrage: 45000,
      verified: false,
      documentUrl: null,
    },
  ];

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/sale-listings/31', {
      statusCode: 200,
      body: mockListing,
    }).as('getSaleDetail');

    cy.intercept('GET', '/api/vehicles/12/photos', {
      statusCode: 200,
      body: mockPhotos,
    }).as('getPhotos');

    cy.intercept('GET', '/api/sale-listings/31/history', {
      statusCode: 200,
      body: mockHistory,
    }).as('getHistory');

    cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
      statusCode: 200,
      body: {
        content: [
          {
            id: 33,
            vehicleId: 14,
            titre: 'Renault Clio 5',
            marque: 'Renault',
            modele: 'Clio 5',
            annee: 2023,
            ville: 'Casablanca',
            prixVente: 145000,
            etat: 'USED',
            statut: 'PUBLISHED',
            photoUrl: 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d',
            kilometrage: 42000,
          },
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 10,
      },
    }).as('getSimilarListings');
  });

  it('affiche les caractéristiques complètes du véhicule et les spécifications', () => {
    visitSaleDetail();

    cy.get('[data-cy="sale-detail-page"]').should('be.visible');
    cy.get('[data-cy="sale-detail-title"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-cy="sale-detail-condition"]').should('contain.text', 'OCCASION');
    cy.get('[data-cy="sale-detail-year"]').should('contain.text', '2022');
    cy.get('[data-cy="sale-detail-vin"]').should('contain.text', 'VF1HJD20260315001');
    cy.get('[data-cy="sale-detail-mileage"]').should('contain.text', '52');
    cy.get('[data-cy="sale-detail-power"]').should('contain.text', '115');
    cy.get('[data-cy="sale-detail-description"]').should('contain.text', 'SUV polyvalent et robuste');
    cy.get('[data-cy="sale-detail-price"]').should('contain.text', '185');
  });

  it('permet de naviguer entre la liste et la vue détaillée', () => {
    cy.intercept('GET', '/api/brands', { statusCode: 200, body: [] });
    cy.intercept('GET', '/api/categories', { statusCode: 200, body: [] });
    cy.intercept('GET', /\/api\/sale-listings\/search(\?.*)?$/, {
      statusCode: 200,
      body: { content: [mockListing], totalElements: 1, totalPages: 1, number: 0, size: 10 },
    }).as('searchSale');

    cy.visit('/sale');
    cy.wait('@searchSale');
    cy.get('[data-cy="sale-details"]').first().click();

    cy.location('pathname').should('eq', '/sale/31');
    cy.wait('@getSaleDetail');
    cy.get('[data-cy="sale-detail-title"]').should('be.visible');

    // Retour à la liste
    cy.get('[data-cy="sale-detail-back"]').click();
    cy.location('pathname').should('eq', '/sale');
  });

  it('affiche la galerie de photos et permet de faire défiler les images', () => {
    visitSaleDetail();
    cy.wait('@getPhotos');

    cy.get('[data-cy="photo-gallery"]').should('be.visible');
    cy.get('[data-cy="gallery-stage"]').should('be.visible');
    cy.get('[data-cy="gallery-active-image"]').should('be.visible');

    // Cliquer sur image suivante
    cy.get('[data-cy="gallery-next"]').click();
    cy.get('[data-cy="gallery-active-image"]')
      .should('have.attr', 'src')
      .and('include', mockPhotos[1].url);

    // Cliquer sur image précédente
    cy.get('[data-cy="gallery-prev"]').click();
    cy.get('[data-cy="gallery-active-image"]')
      .should('have.attr', 'src')
      .and('include', mockPhotos[0].url);
  });

  it('affiche la frise chronologique d historique avec les événements vérifiés', () => {
    visitSaleDetail();
    cy.wait('@getHistory');

    cy.get('[data-cy="sale-history"]').should('be.visible');
    cy.get('[data-cy="sale-history-event"]').should('have.length', 3);

    // Vérifier les détails du premier événement
    cy.get('[data-cy="sale-history-event"]').first().within(() => {
      cy.get('.event-heading').should('be.visible');
      cy.get('p').should('contain.text', 'Première révision annuelle');
      cy.get('.event-meta').should('contain.text', '15');
      cy.get('.event-meta a').should('have.attr', 'href', 'https://example.com/facture1.pdf');
    });
  });

  it('propose le partage WhatsApp et la copie de lien', () => {
    visitSaleDetail();

    cy.get('[data-cy="sale-share-whatsapp"]').should('be.visible');
    cy.get('[data-cy="sale-copy-link"]').should('be.visible').click();
  });

  it('affiche la section des annonces similaires', () => {
    visitSaleDetail();
    cy.wait('@getSimilarListings');

    cy.get('[data-cy="sale-similar"]').should('be.visible');
    cy.get('[data-cy="sale-similar"] [data-cy="sale-card"]').should('have.length', 1);
    cy.get('[data-cy="sale-similar"] [data-cy="sale-title"]').should('contain.text', 'Renault Clio 5');
  });
});
