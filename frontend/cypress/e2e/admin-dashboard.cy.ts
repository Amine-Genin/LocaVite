describe('Dashboard administrateur (/admin/dashboard)', () => {
  let requestRanges: string[];

  beforeEach(() => {
    requestRanges = [];
    cy.fixture('dashboard.json').then((dashboard) => {
      cy.intercept('GET', '**/api/admin/dashboard*', (request) => {
        requestRanges.push(String(request.query['from'] ?? ''));
        request.reply({ statusCode: 200, body: dashboard });
      }).as('getDashboard');
    });
  });

  it('protège la route pour les visiteurs non connectés', () => {
    cy.visit('/admin/dashboard');
    cy.location('pathname').should('eq', '/login');
  });

  it('refuse l’accès à un compte client', () => {
    cy.login('dashboard-client@locavite.ma', 'ClientPass123!', ['CLIENT']);
    cy.visit('/admin/dashboard');
    cy.location('pathname').should('eq', '/login');
  });

  it('affiche les KPIs, les graphiques et les dernières activités', () => {
    cy.loginAdmin();
    cy.visit('/admin/dashboard');
    cy.wait('@getDashboard');

    cy.get('[data-testid="kpi-revenue"]').should('contain.text', '184');
    cy.get('[data-testid="kpi-active-reservations"]').should('contain.text', '18');
    cy.get('[data-testid="kpi-vehicles"]').should('contain.text', '24').and('contain.text', '38');
    cy.get('[data-testid="kpi-occupancy"]').should('contain.text', '62');
    cy.get('[data-testid="kpi-clients"]').should('contain.text', '216');
    cy.get('[data-testid="kpi-sales"]').should('contain.text', '7');

    cy.get('[data-testid="chart-revenue"]').should('be.visible').and('have.attr', 'width');
    cy.get('[data-testid="chart-status"]').should('be.visible');
    cy.get('[data-testid="chart-category"]').should('be.visible');
    cy.get('[data-testid="chart-top-vehicles"]').should('be.visible');
    cy.get('[data-testid="chart-occupancy"]').should('be.visible');

    cy.contains('LV-TEST101').should('be.visible');
    cy.contains('PAY-TEST201').should('be.visible');
    cy.contains('samira@example.com').should('be.visible');
  });

  it('actualise le dashboard quand la période change', () => {
    cy.loginAdmin();
    cy.visit('/admin/dashboard');
    cy.wait('@getDashboard');

    cy.contains('mat-button-toggle', '7 jours').click();
    cy.wait('@getDashboard');
    cy.then(() => {
      expect(requestRanges).to.have.length(2);
      expect(requestRanges[0]).not.to.equal(requestRanges[1]);
    });
  });

  it('permet de choisir une période personnalisée', () => {
    cy.loginAdmin();
    cy.visit('/admin/dashboard');
    cy.wait('@getDashboard');

    cy.contains('mat-button-toggle', 'Personnalisée').click();
    cy.get('[data-testid="dashboard-from"]').should('be.visible').clear().type('2026-10-01');
    cy.get('[data-testid="dashboard-to"]').clear().type('2026-10-31');
    cy.contains('button', 'Appliquer').click();
    cy.wait('@getDashboard').its('request.url').should('contain', 'from=2026-10-01');
  });

  it('permet aussi l’accès à une agence et déclenche l’export PDF', () => {
    cy.login('dashboard-agency@locavite.ma', 'AgencePass123!', ['AGENCE']);
    cy.visit('/admin/dashboard');
    cy.wait('@getDashboard');
    cy.window().then((win) => cy.stub(win, 'print').as('printDashboard'));
    cy.get('[data-testid="dashboard-export"]').click();
    cy.get('@printDashboard').should('have.been.calledOnce');
  });
});
