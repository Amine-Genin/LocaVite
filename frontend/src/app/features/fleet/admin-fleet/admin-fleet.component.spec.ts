import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { AdminFleetComponent } from './admin-fleet.component';
import { FleetService, FleetVehicle } from '../../vehicles/services/fleet.service';
import { ReservationService } from '../../reservations/services/reservation.service';
import { VehicleHistoryModalComponent } from '../vehicle-history-modal/vehicle-history-modal.component';

const MOCK_FLEET: FleetVehicle[] = [
  {
    id: 1,
    matricule: '12345-A-1',
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2024,
    categorie: 'SUV',
    statut: 'PUBLISHED',
    ville: 'Casablanca',
    localisationActuelle: 'Casablanca',
    prochaineReservation: '2026-12-05',
    kilometrage: 24500,
    revenusMois: 8400,
    revenusAnnee: 64200,
    tauxOccupation: 78.5,
    agenceId: 1,
    photoUrl: 'https://example.com/duster.jpg',
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
    localisationActuelle: 'Rabat',
    kilometrage: 31000,
    revenusMois: 5200,
    revenusAnnee: 48000,
    tauxOccupation: 62,
    agenceId: 1,
    photoUrl: 'https://example.com/208.jpg',
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
    kilometrage: 49000,
    revenusMois: 1800,
    revenusAnnee: 32000,
    tauxOccupation: 30,
    agenceId: 1,
    photoUrl: 'https://example.com/clio.jpg',
  },
];

describe('AdminFleetComponent', () => {
  let component: AdminFleetComponent;
  let fixture: ComponentFixture<AdminFleetComponent>;
  let fleetServiceSpy: jasmine.SpyObj<FleetService>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  beforeEach(async () => {
    fleetServiceSpy = jasmine.createSpyObj('FleetService', ['getAll', 'updateStatus', 'getHistory']);
    reservationServiceSpy = jasmine.createSpyObj('ReservationService', ['getAll']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    fleetServiceSpy.getAll.and.returnValue(of(MOCK_FLEET));
    fleetServiceSpy.updateStatus.and.returnValue(
      of({ ...MOCK_FLEET[0], statut: 'MAINTENANCE' }),
    );
    reservationServiceSpy.getAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, size: 200, number: 0 }),
    );

    await TestBed.configureTestingModule({
      imports: [AdminFleetComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: FleetService, useValue: fleetServiceSpy },
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    })
      .overrideComponent(AdminFleetComponent, {
        set: {
          providers: [
            { provide: MatDialog, useValue: dialogSpy },
            { provide: MatSnackBar, useValue: snackBarSpy },
          ],
        },
      })
      .compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(AdminFleetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait charger la flotte et calculer les statistiques en haut de page', () => {
    expect(fleetServiceSpy.getAll).toHaveBeenCalledTimes(1);
    expect(component.stats()).toEqual({
      total: 3,
      available: 1,
      rented: 1,
      maintenance: 1,
    });
  });

  it('devrait mettre à jour le statut et rafraîchir la flotte (changement de statut -> refresh)', () => {
    const updatedList: FleetVehicle[] = [
      { ...MOCK_FLEET[0], statut: 'MAINTENANCE' },
      MOCK_FLEET[1],
      MOCK_FLEET[2],
    ];
    fleetServiceSpy.getAll.and.returnValue(of(updatedList));

    component.onStatusChange({ vehicle: MOCK_FLEET[0], statut: 'MAINTENANCE' });

    expect(fleetServiceSpy.updateStatus).toHaveBeenCalledWith(1, 'MAINTENANCE');
    expect(fleetServiceSpy.getAll).toHaveBeenCalledTimes(2);
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(component.stats().maintenance).toBe(2);
    expect(component.stats().available).toBe(0);
  });

  it('devrait afficher une erreur si la mise à jour du statut échoue', () => {
    fleetServiceSpy.updateStatus.and.returnValue(throwError(() => new Error('HTTP 500')));

    component.onStatusChange({ vehicle: MOCK_FLEET[0], statut: 'MAINTENANCE' });

    expect(component.updatingVehicleId()).toBeNull();
    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(fleetServiceSpy.getAll).toHaveBeenCalledTimes(1);
  });

  it('devrait filtrer les véhicules via la recherche rapide', () => {
    component.searchQuery.set('Duster');
    expect(component.filteredVehicles().length).toBe(1);
    expect(component.filteredVehicles()[0].id).toBe(1);

    component.searchQuery.set('Marrakech');
    expect(component.filteredVehicles().length).toBe(1);
    expect(component.filteredVehicles()[0].id).toBe(3);
  });

  it('devrait basculer entre les vues tableau, grille et calendrier', () => {
    expect(component.viewMode()).toBe('table');

    component.setViewMode('grid');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="fleet-grid"]')).toBeTruthy();

    component.setViewMode('calendar');
    fixture.detectChanges();
    expect(reservationServiceSpy.getAll).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[data-testid="fleet-calendar"]')).toBeTruthy();
  });

  it('devrait ouvrir la modale historique avec le véhicule sélectionné', () => {
    component.onViewHistory(MOCK_FLEET[0]);

    expect(dialogSpy.open).toHaveBeenCalledWith(
      VehicleHistoryModalComponent,
      jasmine.objectContaining({
        data: { vehicle: MOCK_FLEET[0] },
      }),
    );
  });

  it('devrait naviguer vers les détails et ouvrir le calendrier filtré pour un véhicule', () => {
    const navSpy = spyOn(router, 'navigate');
    component.onViewDetails(MOCK_FLEET[1]);
    expect(navSpy).toHaveBeenCalledWith(['/vehicles', 2]);

    component.onViewCalendar(MOCK_FLEET[1]);
    expect(component.viewMode()).toBe('calendar');
    expect(component.selectedCalendarVehicleId()).toBe(2);
  });
});
