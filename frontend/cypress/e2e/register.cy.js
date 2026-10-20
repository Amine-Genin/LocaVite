describe("Page d'inscription (Register)", () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it("affiche tous les éléments du formulaire d'inscription", () => {
    cy.visit('/register');

    cy.contains(/LOCAVITE|LocaVite/).should('be.visible');
    cy.contains('Créer mon compte').should('be.visible');
    cy.get('input[formControlName="nom"]').should('be.visible');
    cy.get('input[formControlName="prenom"]').should('be.visible');
    cy.get('input[formControlName="email"]').should('be.visible');
    cy.get('input[formControlName="telephone"]').should('be.visible');
    cy.get('input[formControlName="password"]').should('be.visible');
    cy.get('input[formControlName="confirmPassword"]').should('be.visible');
    cy.get('button[type="submit"]').should('be.visible');
    cy.contains('Connexion').should('be.visible');
    cy.contains('Un espace sécurisé').should('be.visible');
    cy.contains('Moins de formalités. Plus de liberté.').should('be.visible');
  });

  it('valide côté client et désactive le bouton si les données sont invalides', () => {
    cy.visit('/register');

    // Initialement vide -> désactivé
    cy.get('button[type="submit"]').should('be.disabled');

    // Mot de passe trop faible (sans majuscule/chiffre ou < 8 chars)
    cy.get('input[formControlName="nom"]').type('Benali');
    cy.get('input[formControlName="prenom"]').type('Ahmed');
    cy.get('input[formControlName="email"]').type('ahmed.benali@locavite.ma');
    cy.get('input[formControlName="telephone"]').type('0612345678');
    cy.get('input[formControlName="password"]').type('faible');
    cy.get('input[formControlName="confirmPassword"]').type('faible');
    cy.get('button[type="submit"]').should('be.disabled');

    // Confirmation de mot de passe différente
    cy.get('input[formControlName="password"]').clear().type('Secret123');
    cy.get('input[formControlName="confirmPassword"]').clear().type('AutreChose123');
    cy.get('button[type="submit"]').should('be.disabled');

    // Téléphone invalide
    cy.get('input[formControlName="confirmPassword"]').clear().type('Secret123');
    cy.get('input[formControlName="telephone"]').clear().type('1234');
    cy.get('button[type="submit"]').should('be.disabled');

    // Tout est valide -> activé
    cy.get('input[formControlName="telephone"]').clear().type('0612345678');
    cy.get('button[type="submit"]').should('not.be.disabled');
  });

  it("affiche une erreur si l'email est déjà utilisé (409 Conflict)", () => {
    cy.intercept('POST', '/api/auth/register', {
      statusCode: 409,
      body: { message: 'Cette adresse e-mail est déjà utilisée.' },
    }).as('registerConflict');

    cy.visit('/register');
    cy.get('input[formControlName="nom"]').type('Benali');
    cy.get('input[formControlName="prenom"]').type('Ahmed');
    cy.get('input[formControlName="email"]').type('existant@locavite.ma');
    cy.get('input[formControlName="telephone"]').type('0612345678');
    cy.get('input[formControlName="password"]').type('MotDePasse123');
    cy.get('input[formControlName="confirmPassword"]').type('MotDePasse123');

    cy.get('button[type="submit"]').click();
    cy.wait('@registerConflict');

    cy.contains('Cette adresse e-mail est déjà utilisée.').should('be.visible');
    cy.url().should('include', '/register');
  });

  it("inscrit l'utilisateur avec succès et redirige vers /login avec notification", () => {
    cy.intercept('POST', '/api/auth/register', {
      statusCode: 201,
      body: {
        id: 42,
        nom: 'Benali',
        prenom: 'Ahmed',
        email: 'ahmed.benali@locavite.ma',
        telephone: '0612345678',
        roles: ['CLIENT'],
        createdAt: '2026-10-19T10:00:00',
      },
    }).as('registerSuccess');

    cy.visit('/register');
    cy.get('input[formControlName="nom"]').type('Benali');
    cy.get('input[formControlName="prenom"]').type('Ahmed');
    cy.get('input[formControlName="email"]').type('ahmed.benali@locavite.ma');
    cy.get('input[formControlName="telephone"]').type('0612345678');
    cy.get('input[formControlName="password"]').type('MotDePasse123');
    cy.get('input[formControlName="confirmPassword"]').type('MotDePasse123');

    cy.get('button[type="submit"]').click();
    cy.wait('@registerSuccess');

    // Redirection vers /login
    cy.url().should('include', '/login');
    cy.url().should('include', 'registered=true');
    cy.contains('Connexion').should('be.visible');
  });

  it('prend en charge le support multilingue (FR / AR / EN)', () => {
    cy.visit('/register');

    // Passer en arabe
    cy.contains('العربية').click();
    cy.get('html').should('have.attr', 'dir', 'rtl');
    cy.contains('إنشاء حساب').should('be.visible');

    // Passer en anglais
    cy.contains('EN').click();
    cy.get('html').should('have.attr', 'dir', 'ltr');
    cy.contains('Create account').should('be.visible');

    // Revenir en français
    cy.contains('FR').click();
    cy.get('html').should('have.attr', 'dir', 'ltr');
    cy.contains('Créer mon compte').should('be.visible');
  });

  it('navigue vers la page de connexion via le lien', () => {
    cy.visit('/register');
    cy.contains('Connexion').click();
    cy.url().should('include', '/login');
  });
});
