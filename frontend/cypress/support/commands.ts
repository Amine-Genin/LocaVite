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

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      /**
       * Authenticates a user for E2E tests by setting JWT tokens and user profile in localStorage.
       */
      login(email?: string, password?: string, roles?: string[]): Chainable<void>;

      /**
       * Fills and submits the admin vehicle creation form (/admin/vehicles/new).
       */
      createVehicle(options?: CreateVehicleCommandOptions): Chainable<void>;
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
