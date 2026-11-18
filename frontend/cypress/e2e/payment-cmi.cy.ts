describe('Parcours de paiement CMI en ligne (/payment/:reservationId, /payment/success)', () => {
  const mockReservation = {
    id: 88,
    code: 'RES-8801',
    userId: 2,
    vehicleId: 1,
    vehicle: {
      id: 1,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      carburant: 'DIESEL',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      prixJour: 350,
      ville: 'Casablanca',
      statut: 'PUBLISHED',
    },
    dateDebut: '2026-12-01',
    dateFin: '2026-12-05',
    lieuPriseEnCharge: 'Aéroport Mohammed V - Casablanca',
    lieuRetour: 'Agence LocaVite Casablanca',
    prixTotal: 1400.0,
    statut: 'PENDING',
  };

  beforeEach(() => {
    cy.task('db:reset');
    cy.clearLocalStorage();

    cy.intercept('GET', '/api/reservations/88', {
      statusCode: 200,
      body: mockReservation,
    }).as('getReservation');
  });

  it('redirige vers /login si un visiteur non authentifié accède à la page de paiement', () => {
    cy.visit('/payment/88');
    cy.url().should('include', '/login');
  });

  it('affiche le récapitulatif de réservation et le choix de paiement CMI par défaut', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');
    cy.visit('/payment/88');
    cy.wait('@getReservation');

    // Vérification du récapitulatif de commande
    cy.get('[data-cy="reservation-summary-card"]').should('be.visible');
    cy.get('[data-cy="summary-code"]').should('contain.text', 'RES-8801');
    cy.get('[data-cy="summary-vehicle"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-cy="summary-dates"]').should('contain.text', '01/12/2026');
    cy.get('[data-cy="summary-amount"]').should('contain.text', '1,400.00 MAD');
    cy.get('[data-cy="security-badge"]').should('be.visible');

    // Mode CMI sélectionné par défaut
    cy.get('[data-cy="method-cmi-card"]').should('have.class', 'selected');
    cy.get('[data-cy="submit-payment-btn"]').should('contain.text', '1,400.00 MAD');
  });

  it('exécute le parcours complet CMI avec redirection vers la passerelle et page de confirmation', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    // Chargement de la fixture CMI officielle
    cy.fixture('payment-cmi-success.json').then((cmiFixture) => {
      cy.intercept('POST', '/api/payments/cmi/initiate', (req) => {
        expect(req.body.reservationId).to.eq(88);
        expect(req.body.montant).to.eq(1400);
        req.reply({
          statusCode: 200,
          body: cmiFixture,
        });
      }).as('initiateCmi');
    });

    cy.intercept('GET', '/api/payments/42', {
      statusCode: 200,
      body: {
        id: 42,
        code: 'PAY-CMI042',
        reservationId: 88,
        montant: 1400.0,
        devise: 'MAD',
        methode: 'CMI',
        statut: 'SUCCESS',
        paidAt: '2026-11-18T10:00:00Z',
      },
    }).as('getPaymentStatus');

    cy.visit('/payment/88');
    cy.wait('@getReservation');

    // Sélection CMI et soumission
    cy.get('[data-cy="method-cmi-card"]').click();
    cy.get('[data-cy="submit-payment-btn"]').click();

    cy.wait('@initiateCmi');

    // Redirection automatique vers la passerelle CMI puis vers la page de succès
    cy.url().should('include', '/payment/success');
    cy.wait('@getPaymentStatus');

    // Validation des détails du paiement CMI validé
    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="success-icon"]').should('be.visible');
    cy.get('[data-cy="payment-code"]').should('contain.text', 'PAY-CMI042');
    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
    cy.get('[data-cy="payment-amount"]').should('contain.text', '1,400.00 MAD');

    // Boutons d'action
    cy.get('[data-cy="download-invoice-btn"]').should('be.visible').and('not.be.disabled');
    cy.get('[data-cy="go-to-reservations-btn"]')
      .should('be.visible')
      .and('have.attr', 'href', '/my-reservations');
  });

  it('gère le polling d’état de paiement CMI jusqu’à obtention du statut SUCCESS', () => {
    cy.loginAsClient('ahmed.benali@locavite.ma');

    let callCount = 0;
    cy.intercept('GET', '/api/payments/42', (req) => {
      callCount += 1;
      if (callCount === 1) {
        req.reply({
          statusCode: 200,
          body: {
            id: 42,
            code: 'PAY-CMI042',
            reservationId: 88,
            montant: 1400.0,
            devise: 'MAD',
            methode: 'CMI',
            statut: 'PENDING',
          },
        });
      } else {
        req.reply({
          statusCode: 200,
          body: {
            id: 42,
            code: 'PAY-CMI042',
            reservationId: 88,
            montant: 1400.0,
            devise: 'MAD',
            methode: 'CMI',
            statut: 'SUCCESS',
            paidAt: '2026-11-18T10:05:00Z',
          },
        });
      }
    }).as('pollPaymentStatus');

    cy.visit('/payment/success?paymentId=42&reservationId=88&code=PAY-CMI042&method=CMI');

    cy.wait('@pollPaymentStatus');
    cy.get('[data-cy="payment-success-page"]').should('be.visible');
    cy.get('[data-cy="invoice-number"]').should('contain.text', 'FAC-2026-00042');
  });
});
