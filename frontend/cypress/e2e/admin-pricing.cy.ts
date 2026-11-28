describe('Configuration des tarifs saisonniers et options (/admin/pricing)', () => {
  const initialSeasons = [
    {
      id: 1,
      nom: 'Haute saison été',
      dateDebut: '2026-07-01',
      dateFin: '2026-08-31',
      coefficient: 1.5,
      priorite: 3,
      isRecurring: true,
    },
  ];

  const initialOptions = [
    { id: 1, type: 'GPS', libelle: 'GPS Navigation', prixJour: 50, actif: true },
    { id: 2, type: 'BABY_SEAT', libelle: 'Siège bébé', prixJour: 30, actif: true },
  ];

  const initialDiscounts = [
    {
      id: 1,
      type: 'LONG_DURATION',
      libelle: 'Remise longue durée (> 7 jours)',
      minDays: 8,
      pourcentage: 10,
      actif: true,
    },
    {
      id: 2,
      type: 'PROMO_CODE',
      code: 'ETE2026',
      libelle: 'Code Promo Été',
      pourcentage: 15,
      actif: true,
    },
  ];

  const mockVehiclesPage = {
    content: [
      {
        id: 42,
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2024,
        categorie: 'SUV',
        prixJour: 400,
        ville: 'Casablanca',
        statut: 'PUBLISHED',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 100,
    number: 0,
  };

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/admin/seasons', { statusCode: 200, body: initialSeasons }).as('getSeasons');
    cy.intercept('GET', '/api/admin/options', { statusCode: 200, body: initialOptions }).as('getOptions');
    cy.intercept('GET', '/api/admin/discounts', { statusCode: 200, body: initialDiscounts }).as('getDiscounts');
    cy.intercept('GET', /\/api\/vehicles(\?.*)?$/, { statusCode: 200, body: mockVehiclesPage }).as('getVehicles');
    cy.intercept('POST', '/api/admin/pricing/simulate', {
      statusCode: 200,
      body: {
        sousTotal: 2400,
        options: 200,
        remiseDuree: 0,
        remisePromo: 0,
        remiseTotale: 0,
        caution: 3000,
        tva: 520,
        total: 6120,
        jours: 4,
      },
    }).as('simulatePrice');
  });

  it('protège la route /admin/pricing et refuse un utilisateur CLIENT', () => {
    cy.loginAsClient();
    cy.visit('/admin/pricing');
    cy.url().should('not.include', '/admin/pricing');
  });

  it('affiche les onglets Saisons, Options, Remises et l’aperçu calendrier annuel', () => {
    cy.loginAdmin();
    cy.visit('/admin/pricing');
    cy.wait(['@getSeasons', '@getOptions', '@getDiscounts']);

    cy.get('[data-testid="seasons-table"]').should('contain.text', 'Haute saison été');
    cy.get('[data-testid="annual-calendar"]').should('be.visible');

    cy.contains('.mdc-tab', 'Options').click();
    cy.get('[data-testid="options-table"]').should('contain.text', 'GPS Navigation');

    cy.contains('.mdc-tab', 'Remises').click();
    cy.get('[data-testid="discounts-table"]').should('contain.text', 'ETE2026');
  });

  it('valide le chevauchement côté client et crée une nouvelle saison avec mise à jour du tableau', () => {
    const createdSeason = {
      id: 2,
      nom: 'Pic Août',
      dateDebut: '2026-08-01',
      dateFin: '2026-08-20',
      coefficient: 1.9,
      priorite: 4,
      isRecurring: false,
    };

    cy.intercept('POST', '/api/admin/seasons', { statusCode: 201, body: createdSeason }).as('createSeason');

    cy.loginAdmin();
    cy.visit('/admin/pricing');
    cy.wait(['@getSeasons', '@getOptions', '@getDiscounts']);

    cy.get('[data-testid="add-season-btn"]').click();
    cy.get('[data-testid="season-form-modal"]').should('be.visible');

    cy.get('[data-testid="season-nom-input"]').type('Pic Août');
    cy.get('[data-testid="season-start-input"]').type('8/1/2026').blur();
    cy.get('[data-testid="season-end-input"]').type('8/20/2026').blur();
    cy.get('[data-testid="season-coeff-input"]').clear().type('1.9');
    cy.get('[data-testid="season-priority-input"]').clear().type('3');

    // Même priorité + chevauchement -> bloqué côté client
    cy.get('[data-testid="season-overlap-error"]').should('be.visible');
    cy.get('[data-testid="season-submit-btn"]').should('be.disabled');

    // Priorité distincte -> autorisé
    cy.get('[data-testid="season-priority-input"]').clear().type('4');
    cy.get('[data-testid="season-overlap-error"]').should('not.exist');

    cy.intercept('GET', '/api/admin/seasons', {
      statusCode: 200,
      body: [...initialSeasons, createdSeason],
    }).as('refreshSeasons');

    cy.get('[data-testid="season-submit-btn"]').should('not.be.disabled').click();
    cy.wait('@createSeason');
    cy.wait('@refreshSeasons');
    cy.get('[data-testid="seasons-table"]').should('contain.text', 'Pic Août');
  });

  it('permet de basculer le statut actif d’une option et simule le prix en temps réel', () => {
    cy.intercept('PUT', '/api/admin/options/1', {
      statusCode: 200,
      body: { ...initialOptions[0], actif: false },
    }).as('toggleOption');

    cy.loginAdmin();
    cy.visit('/admin/pricing');
    cy.wait(['@getSeasons', '@getOptions', '@getDiscounts', '@simulatePrice']);

    // Vérifier le simulateur de prix
    cy.get('[data-testid="sim-total"]').should('contain.text', '6,120.00 MAD');
    cy.get('[data-testid="sim-option-GPS"] input[type="checkbox"]').check({ force: true });
    cy.wait('@simulatePrice');

    // Basculer une option dans l'onglet Options
    cy.contains('.mdc-tab', 'Options').click();
    cy.get('[data-testid="option-toggle-1"] button[role="switch"]').click({ force: true });
    cy.wait('@toggleOption');
  });
});
