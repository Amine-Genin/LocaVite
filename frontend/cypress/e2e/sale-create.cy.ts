describe('Create sale listing (/admin/sale/new)', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('requires agency authentication', () => {
    cy.visit('/admin/sale/new');
    cy.location('pathname').should('eq', '/login');
  });
});
