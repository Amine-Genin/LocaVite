describe('Profil client (Angular)', () => {
  beforeEach(() => {
    // Nettoyer le localStorage avant chaque test
    cy.clearLocalStorage();
  });

  it('redirection vers /login si non authentifié', () => {
    cy.visit('/profile');
    cy.url().should('include', '/login');
    cy.contains('Connexion').should('be.visible');
  });

  it('connexion et affichage complet du profil client', () => {
    // Intercepter l'authentification et le profil pour un test E2E fiable
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: {
        accessToken: 'fake-jwt-token',
        refreshToken: 'fake-refresh-token',
        expiresIn: 86400000,
        user: {
          id: 1,
          nom: 'Benali',
          prenom: 'Ahmed',
          email: 'ahmed.benali@locavite.ma',
          telephone: '0612345678',
          roles: ['CLIENT'],
          createdAt: '2026-01-15T10:00:00',
        },
      },
    }).as('login');

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    }).as('getProfile');

    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [
        {
          id: 10,
          type: 'PERMIS',
          status: 'VALIDATED',
          url: 'http://localhost:9000/locavite-documents/permis.pdf',
          uploadedAt: '2026-01-16T10:00:00',
        },
        {
          id: 11,
          type: 'CIN',
          status: 'PENDING',
          url: 'http://localhost:9000/locavite-documents/cin.pdf',
          uploadedAt: '2026-01-16T11:00:00',
        },
      ],
    }).as('getDocs');

    cy.visit('/login');
    cy.get('input[formControlName="email"]').type('ahmed.benali@locavite.ma');
    cy.get('input[formControlName="password"]').type('Motdepasse123!');
    cy.get('button[type="submit"]').click();

    cy.wait('@login');
    cy.wait('@getProfile');
    cy.wait('@getDocs');

    cy.url().should('include', '/profile');

    // Vérifier l'affichage des données
    cy.contains('Mon profil').should('be.visible');
    cy.contains('Benali').should('be.visible');
    cy.contains('Ahmed').should('be.visible');
    cy.contains('ahmed.benali@locavite.ma').should('be.visible');
    cy.contains('0612345678').should('be.visible');
    cy.contains('CLIENT').should('be.visible');

    // Vérifier les formulaires d'édition
    cy.get('input[formControlName="lastName"]').should('have.value', 'Benali');
    cy.get('input[formControlName="firstName"]').should('have.value', 'Ahmed');
    cy.get('input[formControlName="phone"]').should('have.value', '0612345678');

    // Vérifier les documents et statuts
    cy.contains('Permis de conduire').should('be.visible');
    cy.contains('Validé').should('be.visible');
    cy.contains('CIN').should('be.visible');
    cy.contains('En attente').should('be.visible');
  });

  it('modification du profil avec message de succès', () => {
    // Pré-authentifier via localStorage
    localStorage.setItem('accessToken', 'mock-token');

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    }).as('getProfile');

    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [],
    }).as('getDocs');

    cy.intercept('PUT', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        nom: 'Benali',
        prenom: 'Karim',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0698765432',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    }).as('updateProfile');

    cy.visit('/profile');
    cy.wait('@getProfile');

    // Modifier le prénom et le téléphone
    cy.get('input[formControlName="firstName"]').clear().type('Karim');
    cy.get('input[formControlName="phone"]').clear().type('0698765432');
    cy.get('button[type="submit"]').contains('Enregistrer').click();

    cy.wait('@updateProfile');
    cy.contains('Profil mis à jour.').should('be.visible');
  });

  it('déconnexion', () => {
    localStorage.setItem('accessToken', 'mock-token');

    cy.intercept('GET', '/api/users/me', {
      statusCode: 200,
      body: {
        id: 1,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-01-15T10:00:00',
      },
    });
    cy.intercept('GET', '/api/users/me/documents', { statusCode: 200, body: [] });
    cy.intercept('POST', '/api/auth/logout', { statusCode: 204, body: null });

    cy.visit('/profile');
    cy.contains('Déconnexion').click();
    cy.url().should('include', '/login');
  });
});

