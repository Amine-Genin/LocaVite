describe('Vehicle Detail Page (/vehicles/:id)', () => {
  const mockVehicle = {
    id: 1,
    brandId: 10,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2024,
    categoryId: 2,
    categorie: 'SUV',
    ville: 'Casablanca',
    prixJour: 350,
    prixVente: 195000,
    carburant: 'DIESEL',
    transmission: 'MANUELLE',
    places: 5,
    description:
      'SUV compact économique, polyvalent, parfait pour vos déplacements en ville et sur autoroute au Maroc.',
    matricule: '78945-A-1',
    statut: 'PUBLISHED',
    agenceId: 101,
    agenceNom: 'LocaVite Casablanca Marina',
    agenceTelephone: '+212600000014',
    photoUrl: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d',
    photos: [
      {
        id: 1,
        vehicleId: 1,
        url: 'https://images.unsplash.com/photo-1541899481282-d53bffe3c35d',
        isPrimary: true,
      },
      {
        id: 2,
        vehicleId: 1,
        url: 'https://images.unsplash.com/photo-1503376780353-7e6692767b70',
        isPrimary: false,
      },
      {
        id: 3,
        vehicleId: 1,
        url: 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf',
        isPrimary: false,
      },
    ],
  };

  const mockAvailability = {
    available: [{ from: '2026-11-01', to: '2026-11-15' }],
    unavailable: [{ from: '2026-11-16', to: '2026-11-20', reason: 'RESERVED' }],
  };

  const mockSimilarVehicles = {
    content: [
      {
        id: 1,
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2024,
        categorie: 'SUV',
        ville: 'Casablanca',
        prixJour: 350,
      },
      {
        id: 2,
        marque: 'Renault',
        modele: 'Captur',
        annee: 2023,
        categorie: 'SUV',
        ville: 'Casablanca',
        prixJour: 400,
        photoUrl: 'https://example.com/captur.jpg',
      },
      {
        id: 3,
        marque: 'Peugeot',
        modele: '2008',
        annee: 2024,
        categorie: 'SUV',
        ville: 'Rabat',
        prixJour: 420,
        photoUrl: 'https://example.com/2008.jpg',
      },
      {
        id: 4,
        marque: 'Hyundai',
        modele: 'Tucson',
        annee: 2024,
        categorie: 'SUV',
        ville: 'Marrakech',
        prixJour: 600,
        photoUrl: 'https://example.com/tucson.jpg',
      },
    ],
    page: { size: 10, number: 0, totalElements: 4, totalPages: 1 },
    totalElements: 4,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(() => {
    cy.intercept('GET', '/api/vehicles/1/availability*', {
      statusCode: 200,
      body: mockAvailability,
    }).as('getAvailability');

    cy.intercept('GET', '/api/vehicles/1', {
      statusCode: 200,
      body: mockVehicle,
    }).as('getVehicle');

    cy.intercept('GET', '/api/vehicles?*categorie=SUV*', {
      statusCode: 200,
      body: mockSimilarVehicles,
    }).as('getSimilar');

    cy.intercept('GET', '/api/vehicles', {
      statusCode: 200,
      body: mockSimilarVehicles,
    });
  });

  it('displays full vehicle specifications, pricing, city and agency on /vehicles/1', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');

    // Specifications
    cy.get('[data-cy="vehicle-brand"]').should('contain.text', 'Dacia');
    cy.get('[data-cy="vehicle-model"]').should('contain.text', 'Duster');
    cy.get('[data-cy="vehicle-year"]').should('contain.text', '2024');
    cy.get('[data-cy="vehicle-category"]').should('contain.text', 'SUV');
    cy.get('[data-cy="vehicle-fuel"]').should('contain.text', 'DIESEL');
    cy.get('[data-cy="vehicle-transmission"]').should('contain.text', 'MANUELLE');
    cy.get('[data-cy="vehicle-seats"]').should('contain.text', '5');
    cy.get('[data-cy="vehicle-description"]').should('contain.text', 'SUV compact');
    cy.get('[data-cy="vehicle-matricule"]').should('contain.text', '78945-A-1');

    // Pricing
    cy.get('[data-cy="vehicle-rent-price"]').should('contain.text', '350');
    cy.get('[data-cy="vehicle-sale-price"]').should('contain.text', '195000');

    // City and Agency
    cy.get('[data-cy="vehicle-city"]').should('contain.text', 'Casablanca');
    cy.get('[data-cy="vehicle-agency"]').should('contain.text', 'LocaVite Casablanca Marina');
  });

  it('navigates through photo gallery with buttons and thumbnails', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="photo-gallery"]').should('be.visible');
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '1 / 3');

    // Next button
    cy.get('[data-cy="gallery-next"]').click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '2 / 3');

    // Next button again
    cy.get('[data-cy="gallery-next"]').click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '3 / 3');

    // Thumbnail click to go back to 1st photo
    cy.get('[data-cy="gallery-thumbnail"]').first().click();
    cy.get('[data-cy="gallery-counter"]').should('contain.text', '1 / 3');
  });

  it('displays the monthly availability calendar and highlights dates', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    // Switch to Availability tab if needed
    cy.get('[data-cy="vehicle-tabs"]').contains('Disponibilité').click();
    cy.get('[data-cy="availability-calendar"]').should('be.visible');
    cy.get('[data-cy="calendar-legend"]').should('be.visible');
    cy.get('[data-cy="mat-calendar"]').should('be.visible');
  });

  it('navigates to /reservation/:id when clicking Réserver', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="reserve-btn"]').should('be.visible').click();
    cy.url().should('include', '/reservation/1');
    cy.get('[data-cy="reservation-page"]').should('be.visible');
  });

  it('opens contact modal with WhatsApp and contact form on clicking Contacter agence', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="contact-agency-btn"]').click();
    cy.get('[data-cy="contact-dialog"]').should('be.visible');

    // WhatsApp action button
    cy.get('[data-cy="whatsapp-contact-btn"]').should('be.visible');

    // Contact form submit
    cy.get('[data-cy="contact-name-input"]').type('Yassine Alami');
    cy.get('[data-cy="contact-email-input"]').type('yassine@example.com');
    cy.get('[data-cy="contact-phone-input"]').type('0612345678');

    cy.get('[data-cy="send-contact-btn"]').click();
    cy.get('[data-cy="contact-success-alert"]').should('be.visible');

    cy.get('[data-cy="contact-dialog"] button').contains('Fermer').click();
    cy.get('[data-cy="contact-dialog"]').should('not.exist');
  });

  it('toggles favorite button on vehicle detail page', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="favorite-btn"]')
      .first()
      .should('have.attr', 'aria-pressed', 'false')
      .click()
      .should('have.attr', 'aria-pressed', 'true')
      .and('have.class', 'active');
  });

  it('displays 3 similar vehicle suggestions excluding current vehicle', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.get('[data-cy="similar-vehicles"]').should('be.visible');
    cy.get('[data-cy="similar-vehicles"] [data-cy="vehicle-card"]').should('have.length', 3);

    // Current vehicle #1 should not be among similar cards
    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Renault');
    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Peugeot');
    cy.get('[data-cy="similar-vehicles"]').should('contain.text', 'Hyundai');
  });

  it('sets dynamic SEO title and meta tags', () => {
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');

    cy.title().should('contain', 'Dacia Duster (2024)');
    cy.get('meta[name="description"]').should('have.attr', 'content').and('include', 'SUV compact');

    cy.get('meta[property="og:title"]')
      .should('have.attr', 'content')
      .and('include', 'Dacia Duster');
  });

  it('adapts responsive layout across mobile, tablet, and desktop', () => {
    // Mobile Viewport
    cy.viewport(375, 667);
    cy.visit('/vehicles/1');
    cy.wait('@getVehicle');
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');
    cy.get('[data-cy="reserve-btn"]').should('be.visible');

    // Tablet Viewport
    cy.viewport(768, 1024);
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');

    // Desktop Viewport
    cy.viewport(1280, 800);
    cy.get('[data-cy="vehicle-detail-page"]').should('be.visible');
    cy.get('[data-cy="action-card"]').should('be.visible');
  });
});
