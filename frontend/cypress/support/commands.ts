export interface CreateVehicleCommandOptions {
  brandSearch?: string;
  marque?: string;
  modele?: string;
  annee?: number;
  categorie?: string;
  carburant?: 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE';
  transmission?: 'MANUELLE' | 'AUTOMATIQUE';
  places?: number;
  matricule?: string;
  description?: string;
  prixJour?: number;
  prixVente?: number;
  ville?: string;
  statut?: 'PUBLISHED' | 'DRAFT';
  withPhoto?: boolean;
}

export interface CreateReservationCommandOptions {
  dateDebutDays?: number;
  dateFinDays?: number;
  pickupType?: 'Aéroport' | 'Gare' | 'Hôtel' | 'Agence LocaVite';
  pickupLocation?: string;
  returnLocation?: string;
  gps?: boolean;
  babySeat?: boolean;
  extraDriver?: boolean;
  acceptTerms?: boolean;
  submit?: boolean;
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Authenticates a user for E2E tests by setting JWT tokens and user profile in localStorage.
       */
      login(email?: string, password?: string, roles?: string[]): Chainable<void>;

      /**
       * Authenticates a client user for reservation E2E tests.
       */
      loginAsClient(email?: string, password?: string, profile?: Record<string, unknown>): Chainable<void>;

      /**
       * Fills and submits the admin vehicle creation form (/admin/vehicles/new).
       */
      createVehicle(options?: CreateVehicleCommandOptions): Chainable<void>;

      /**
       * Fills the reservation stepper steps and optionally submits the reservation.
       */
      createReservation(options?: CreateReservationCommandOptions): Chainable<void>;
    }
  }
}

const createMockJwt = (email: string, roles: string[]): string => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(
    JSON.stringify({
      sub: email,
      roles,
      exp: Math.floor(Date.now() / 1000) + 3600,
    }),
  );
  return `${header}.${payload}.fakesignature`;
};

Cypress.Commands.add(
  'login',
  (
    email = 'admin.dev@locavite.ma',
    _password = 'AdminDev2026!',
    roles: string[] = ['AGENCE', 'ADMIN'],
  ) => {
    const token = createMockJwt(email, roles);
    const user = {
      id: roles.includes('CLIENT') ? 2 : 1,
      nom: roles.includes('CLIENT') ? 'Client' : 'Agence',
      prenom: roles.includes('CLIENT') ? 'Test' : 'Casablanca',
      email,
      telephone: '0612345678',
      roles,
      createdAt: '2026-01-15T10:00:00',
    };

    cy.window({ log: false }).then((win) => {
      win.localStorage.setItem('accessToken', token);
      win.localStorage.setItem('refreshToken', 'mock-refresh-token');
      win.localStorage.setItem('currentUser', JSON.stringify(user));
    });

    cy.on('window:before:load', (win) => {
      win.localStorage.setItem('accessToken', token);
      win.localStorage.setItem('refreshToken', 'mock-refresh-token');
      win.localStorage.setItem('currentUser', JSON.stringify(user));
    });
  },
);

Cypress.Commands.add(
  'loginAsClient',
  (
    email = 'ahmed.benali@locavite.ma',
    _password = 'ClientPass123!',
    profile = {},
  ) => {
    const roles = ['CLIENT'];
    const token = createMockJwt(email, roles);
    const user = {
      id: 2,
      nom: 'Benali',
      prenom: 'Ahmed',
      email,
      telephone: '0612345678',
      roles,
      createdAt: '2026-01-15T10:00:00',
      ...profile,
    };

    cy.window({ log: false }).then((win) => {
      win.localStorage.setItem('accessToken', token);
      win.localStorage.setItem('refreshToken', 'mock-refresh-token');
      win.localStorage.setItem('currentUser', JSON.stringify(user));
    });

    cy.on('window:before:load', (win) => {
      win.localStorage.setItem('accessToken', token);
      win.localStorage.setItem('refreshToken', 'mock-refresh-token');
      win.localStorage.setItem('currentUser', JSON.stringify(user));
    });
  },
);

Cypress.Commands.add('createVehicle', (options: CreateVehicleCommandOptions = {}) => {
  const {
    brandSearch = 'ren',
    marque = 'Renault',
    modele = 'Clio 5',
    annee = 2023,
    categorie = 'Économique',
    carburant = 'ESSENCE',
    transmission = 'MANUELLE',
    places = 5,
    matricule = '12345-A-1',
    description = 'Superbe citadine très bien entretenue',
    prixJour = 350,
    prixVente = 145000,
    ville = 'Casablanca',
    statut = 'PUBLISHED',
    withPhoto = true,
  } = options;

  cy.get('[data-cy="brand-input"]').type(brandSearch, { force: true });
  cy.get('[data-cy="brand-option"]').contains(marque).click();

  cy.get('[data-cy="modele-input"]').type(modele, { force: true });

  cy.get('[data-cy="annee-select"]').click({ force: true });
  cy.get('[data-cy="annee-option"]').contains(String(annee)).click();

  cy.get('[data-cy="category-select"]').click({ force: true });
  cy.get('[data-cy="category-option"]').contains(categorie).click();

  cy.get(`[data-cy="carburant-radio-${carburant.toLowerCase()}"]`).click();
  cy.get(`[data-cy="transmission-radio-${transmission.toLowerCase()}"]`).click();

  cy.get('[data-cy="places-input"]').clear({ force: true }).type(String(places), { force: true });
  cy.get('[data-cy="matricule-input"]').type(matricule, { force: true });
  cy.get('[data-cy="description-input"]').type(description, { force: true });

  cy.get('[data-cy="prix-jour-input"]').type(String(prixJour), { force: true });
  cy.get('[data-cy="prix-vente-input"]').type(String(prixVente), { force: true });

  cy.get('[data-cy="ville-select"]').click({ force: true });
  cy.get('[data-cy="ville-option"]').contains(ville).click();

  if (withPhoto) {
    cy.get('[data-cy="file-input"]').selectFile(
      {
        contents: Cypress.Buffer.from('photo-data'),
        fileName: 'vehicle-front.jpg',
        mimeType: 'image/jpeg',
      },
      { force: true },
    );
    cy.get('[data-cy="photo-preview"]').should('have.length.at.least', 1);
  }

  if (statut === 'DRAFT') {
    cy.get('[data-cy="draft-button"]').should('not.be.disabled').click({ force: true });
  } else {
    cy.get('[data-cy="publish-button"]').should('not.be.disabled').click({ force: true });
  }
});

Cypress.Commands.add('createReservation', (options: CreateReservationCommandOptions = {}) => {
  const {
    dateDebutDays = 2,
    dateFinDays = 6,
    pickupType = 'Aéroport',
    pickupLocation = 'Aéroport - Casablanca',
    returnLocation = 'Agence LocaVite Casablanca',
    gps = true,
    babySeat = false,
    extraDriver = false,
    acceptTerms = true,
    submit = true,
  } = options;

  const formatDate = (days: number) => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + days);
    const month = d.getMonth() + 1;
    const day = d.getDate();
    const year = d.getFullYear();
    return `${month}/${day}/${year}`;
  };

  // Étape 1 : Dates
  cy.get('[data-cy="date-debut-input"]').clear().type(formatDate(dateDebutDays)).blur();
  cy.get('[data-cy="date-fin-input"]').clear().type(formatDate(dateFinDays)).blur();
  cy.get('[data-cy="step-dates-next"]').should('not.be.disabled').click();

  // Étape 2 : Lieux
  if (pickupType) {
    cy.get(`[data-cy="pickup-type-${pickupType}"] input[type="radio"]`).check({ force: true });
  }
  if (pickupLocation) {
    cy.get('[data-cy="pickup-location-input"]').clear({ force: true }).type(pickupLocation, { force: true });
  }
  if (returnLocation) {
    cy.get('[data-cy="return-location-input"]').clear({ force: true }).type(returnLocation, { force: true });
  }
  cy.get('[data-cy="step-places-next"]').should('not.be.disabled').click();

  // Étape 3 : Options
  if (gps) {
    cy.get('[data-cy="option-gps"] input[type="checkbox"]').check({ force: true });
  }
  if (babySeat) {
    cy.get('[data-cy="option-baby-seat"] input[type="checkbox"]').check({ force: true });
  }
  if (extraDriver) {
    cy.get('[data-cy="option-extra-driver"] input[type="checkbox"]').check({ force: true });
  }
  cy.get('[data-cy="step-options-next"]').click();

  // Étape 4 : Récapitulatif
  cy.get('[data-cy="step-summary-next"]').should('not.be.disabled').click();

  // Étape 5 : Confirmation
  if (acceptTerms) {
    cy.get('[data-cy="accept-terms-checkbox"] input[type="checkbox"]').check({ force: true });
  }

  if (submit) {
    cy.get('[data-cy="confirm-reservation-btn"]').should('not.be.disabled').click();
  }
});
