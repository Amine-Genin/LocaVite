describe('Gestion de la flotte back-office (/admin/fleet)', () => {
  const todayOffset = (days: number): string => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  };

  const mockFleet = [
    {
      id: 1,
      matricule: '12345-A-1',
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      statut: 'PUBLISHED',
      ville: 'Casablanca',
      localisationActuelle: 'Aéroport Mohammed V',
      prochaineReservationId: 101,
      prochaineReservation: todayOffset(3),
      kilometrage: 24500,
      revenusMois: 8400,
      revenusAnnee: 64200,
      tauxOccupation: 78.5,
      agenceId: 3,
    },
    {
      id: 2,
      matricule: '67890-B-2',
      marque: 'Peugeot',
      modele: '208',
      annee: 2023,
      categorie: 'Économique',
      statut: 'RENTED',
      ville: 'Rabat',
      localisationActuelle: 'Agence Rabat Centre',
      prochaineReservationId: null,
      prochaineReservation: null,
      kilometrage: 31000,
      revenusMois: 5200,
      revenusAnnee: 48000,
      tauxOccupation: 62,
      agenceId: 3,
    },
    {
      id: 3,
      matricule: '55555-C-3',
      marque: 'Renault',
      modele: 'Clio 5',
      annee: 2022,
      categorie: 'Économique',
      statut: 'MAINTENANCE',
      ville: 'Marrakech',
      localisationActuelle: 'Atelier Marrakech',
      prochaineReservationId: null,
      prochaineReservation: null,
      kilometrage: 49000,
      revenusMois: 1800,
      revenusAnnee: 32000,
      tauxOccupation: 30,
      agenceId: 3,
    },
  ];

  const mockHistory = [
    {
      type: 'STATUT',
      description: 'Remise en service après révision',
      dateHeure: '2026-11-25T10:30:00Z',
      ancienStatut: 'MAINTENANCE',
      nouveauStatut: 'PUBLISHED',
    },
    {
      type: 'MAINTENANCE',
      description: 'Vidange complète et plaquettes de frein',
      dateHeure: '2026-11-20T09:00:00Z',
      kilometrage: 24500,
    },
  ];

  const mockReservations = {
    content: [
      {
        id: 101,
        code: 'LV-FLT01',
        vehicleId: 1,
        clientNom: 'Karim Idrissi',
        dateDebut: todayOffset(1),
        dateFin: todayOffset(4),
        prixTotal: 1600,
        statut: 'CONFIRMED',
      },
      {
        id: 102,
        code: 'LV-FLT02',
        vehicleId: 2,
        clientNom: 'Sara Alami',
        dateDebut: todayOffset(2),
        dateFin: todayOffset(6),
        prixTotal: 2100,
        statut: 'ACTIVE',
      },
    ],
    totalElements: 2,
    totalPages: 1,
    size: 200,
    number: 0,
  };

  beforeEach(() => {
    cy.clearLocalStorage();

    cy.intercept('GET', /\/api\/admin\/fleet(\?.*)?$/, {
      statusCode: 200,
      body: mockFleet,
    }).as('getFleet');

    cy.intercept('GET', /\/api\/reservations(\?.*)?$/, {
      statusCode: 200,
      body: mockReservations,
    }).as('getReservations');
  });

  it('protège la route /admin/fleet et refuse les clients non autorisés', () => {
    cy.loginAsClient();
    cy.visit('/admin/fleet');
    cy.url().should('not.include', '/admin/fleet');
  });

  it('affiche les statistiques KPI et le tableau de la flotte', () => {
    cy.loginAsAgence();
    cy.visit('/admin/fleet');
    cy.wait('@getFleet');

    cy.get('[data-testid="stat-total"]').should('contain.text', '3');
    cy.get('[data-testid="stat-available"]').should('contain.text', '1');
    cy.get('[data-testid="stat-rented"]').should('contain.text', '1');
    cy.get('[data-testid="stat-maintenance"]').should('contain.text', '1');

    cy.get('[data-testid="fleet-table"]').should('be.visible');
    cy.get('[data-testid="fleet-row-1"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-testid="fleet-row-1"]').should('contain.text', '12345-A-1');
    cy.get('[data-testid="fleet-row-2"]').should('contain.text', 'Peugeot 208');
  });

  it('permet de basculer entre la vue tableau et la vue grille et de rechercher rapidement', () => {
    cy.loginAsAgence();
    cy.visit('/admin/fleet');
    cy.wait('@getFleet');

    cy.get('[data-testid="view-toggle-grid"]').click();
    cy.get('[data-testid="fleet-grid"]').should('be.visible');
    cy.get('[data-testid="fleet-card-1"]').should('contain.text', 'Dacia Duster');

    cy.get('[data-testid="fleet-search-input"]').type('Peugeot', { force: true });
    cy.get('[data-testid="fleet-card-2"]').should('be.visible');
    cy.get('[data-testid="fleet-card-1"]').should('not.exist');

    cy.get('[data-testid="view-toggle-table"]').click();
    cy.get('[data-testid="fleet-table"]').should('be.visible');
  });

  it('permet de changer le statut d’un véhicule et rafraîchit la flotte', () => {
    const updatedFleet = mockFleet.map((v) =>
      v.id === 1 ? { ...v, statut: 'MAINTENANCE' } : v,
    );

    cy.intercept('PUT', '/api/admin/vehicles/1/status', {
      statusCode: 200,
      body: updatedFleet[0],
    }).as('updateStatus');

    cy.loginAsAgence();
    cy.visit('/admin/fleet');
    cy.wait('@getFleet');

    cy.intercept('GET', /\/api\/admin\/fleet(\?.*)?$/, {
      statusCode: 200,
      body: updatedFleet,
    }).as('refreshFleet');

    cy.get('[data-testid="status-dropdown-1"]').click();
    cy.get('[data-testid="status-option-1-MAINTENANCE"]').click();

    cy.wait('@updateStatus').its('request.body').should('deep.include', { statut: 'MAINTENANCE' });
    cy.wait('@refreshFleet');
    cy.get('[data-testid="stat-maintenance"]').should('contain.text', '2');
  });

  it('ouvre la modale historique timeline d’un véhicule', () => {
    cy.intercept('GET', '/api/admin/vehicles/1/history', {
      statusCode: 200,
      body: mockHistory,
    }).as('getHistory');

    cy.loginAsAgence();
    cy.visit('/admin/fleet');
    cy.wait('@getFleet');

    cy.get('[data-testid="actions-menu-1"]').click();
    cy.get('[data-testid="action-history-1"]').click();

    cy.wait('@getHistory');
    cy.get('[data-testid="vehicle-history-modal"]').should('be.visible');
    cy.get('[data-testid="history-timeline-item"]').should('have.length', 2);
    cy.contains('Vidange complète et plaquettes de frein').should('be.visible');
    cy.get('[data-testid="history-close-btn"]').click();
    cy.get('[data-testid="vehicle-history-modal"]').should('not.exist');
  });

  it('affiche la vue calendrier globale Gantt et le filtre calendrier par véhicule', () => {
    cy.loginAsAgence();
    cy.visit('/admin/fleet');
    cy.wait('@getFleet');

    cy.get('[data-testid="view-toggle-calendar"]').click();
    cy.wait('@getReservations');

    cy.get('[data-testid="fleet-calendar"]').should('be.visible');
    cy.get('[data-testid="gantt-bar-101"]').should('be.visible');
    cy.get('[data-testid="gantt-bar-102"]').should('be.visible');

    // Retour tableau puis ouverture du calendrier filtré sur le véhicule 1
    cy.get('[data-testid="view-toggle-table"]').click();
    cy.get('[data-testid="actions-menu-1"]').click();
    cy.get('[data-testid="action-calendar-1"]').click();

    cy.get('[data-testid="calendar-vehicle-filter"]').should('contain.text', 'Dacia Duster');
    cy.get('[data-testid="calendar-row-1"]').should('be.visible');
    cy.get('[data-testid="calendar-row-2"]').should('not.exist');
  });
});
