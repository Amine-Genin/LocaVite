describe('Page de connexion (Login)', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('affiche tous les éléments du formulaire de connexion', () => {
    cy.visit('/login');

    cy.contains(/LOCAVITE|LocaVite/).should('be.visible');
    cy.contains('Connexion').should('be.visible');
    cy.get('input[formControlName="email"]').should('be.visible');
    cy.get('input[formControlName="password"]').should('be.visible');
    cy.get('mat-checkbox[formControlName="rememberMe"]').should('be.visible');
    cy.contains(/Rester connecté sur cet appareil|Se souvenir de moi/).should('be.visible');
    cy.get('button[type="submit"]').should('be.visible');
    cy.contains('Créer un compte').should('be.visible');
    cy.contains('Un espace sécurisé').should('be.visible');
    cy.contains('Moins de formalités. Plus de liberté.').should('be.visible');
  });

  it('désactive le bouton de soumission si le formulaire est invalide', () => {
    cy.visit('/login');

    // Initialement vide -> bouton désactivé
    cy.get('button[type="submit"]').should('be.disabled');

    // Email invalide -> bouton toujours désactivé
    cy.get('input[formControlName="email"]').type('invalide-email');
    cy.get('input[formControlName="password"]').type('secret');
    cy.get('button[type="submit"]').should('be.disabled');

    // Email valide et mot de passe présent -> bouton activé
    cy.get('input[formControlName="email"]').clear().type('client@locavite.ma');
    cy.get('button[type="submit"]').should('not.be.disabled');
  });

  it('affiche un message derreur lors dun échec de connexion (mauvais identifiants)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 401,
      body: { message: 'Identifiants incorrects' },
    }).as('loginFail');

    cy.visit('/login');
    cy.get('input[formControlName="email"]').type('inconnu@locavite.ma');
    cy.get('input[formControlName="password"]').type('MauvaisMotDePasse1');
    cy.get('button[type="submit"]').click();

    cy.wait('@loginFail');
    cy.contains('Identifiants incorrects').should('be.visible');
    cy.url().should('include', '/login');
  });

  it('se connecte avec succès, stocke le JWT et redirige', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: {
        accessToken: 'fake-jwt-token-123',
        refreshToken: 'fake-refresh-token-456',
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
    }).as('loginSuccess');

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
    }).as('getMe');

    cy.intercept('GET', '/api/users/me/documents', {
      statusCode: 200,
      body: [],
    }).as('getDocs');

    cy.visit('/login');
    cy.get('input[formControlName="email"]').type('ahmed.benali@locavite.ma');
    cy.get('input[formControlName="password"]').type('Motdepasse123!');
    cy.get('mat-checkbox[formControlName="rememberMe"]').click();
    cy.get('button[type="submit"]').click();

    cy.wait('@loginSuccess');

    // Vérifier le stockage du token dans localStorage
    cy.window().then((win) => {
      expect(win.localStorage.getItem('accessToken')).to.equal('fake-jwt-token-123');
      expect(win.localStorage.getItem('refreshToken')).to.equal('fake-refresh-token-456');
    });

    // Redirection vers le dashboard / profile
    cy.url().should('match', /\/(dashboard|profile)/);
  });

  it('prend en charge le support multilingue (FR / AR / EN)', () => {
    cy.visit('/login');

    // Passer en arabe
    cy.contains('العربية').click();
    cy.get('html').should('have.attr', 'dir', 'rtl');
    cy.contains('تسجيل الدخول').should('be.visible');

    // Passer en anglais
    cy.contains('EN').click();
    cy.get('html').should('have.attr', 'dir', 'ltr');
    cy.contains('Sign in').should('be.visible');

    // Revenir en français
    cy.contains('FR').click();
    cy.get('html').should('have.attr', 'dir', 'ltr');
    cy.contains('Connexion').should('be.visible');
  });

  it('navigue vers la page dinscription via le lien', () => {
    cy.visit('/login');
    cy.contains('Créer un compte').click();
    cy.url().should('include', '/register');
  });
});
