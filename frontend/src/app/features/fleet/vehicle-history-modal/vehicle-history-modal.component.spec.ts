import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import {
  VehicleHistoryModalComponent,
  VehicleHistoryModalData,
} from './vehicle-history-modal.component';
import {
  FleetHistory,
  FleetService,
  FleetVehicle,
} from '../../vehicles/services/fleet.service';

const MOCK_VEHICLE: FleetVehicle = {
  id: 1,
  matricule: '12345-A-1',
  marque: 'Dacia',
  modele: 'Duster',
  annee: 2024,
  categorie: 'SUV',
  statut: 'PUBLISHED',
  ville: 'Casablanca',
  localisationActuelle: 'Casablanca',
  kilometrage: 24500,
  revenusMois: 8400,
  revenusAnnee: 64200,
  tauxOccupation: 78.5,
  agenceId: 1,
  photoUrl: 'https://example.com/duster.jpg',
};

const MOCK_HISTORY: FleetHistory[] = [
  {
    type: 'STATUT',
    description: 'Retour de révision',
    dateHeure: '2026-11-25T10:30:00Z',
    ancienStatut: 'MAINTENANCE',
    nouveauStatut: 'PUBLISHED',
  },
  {
    type: 'MAINTENANCE',
    description: 'Vidange + changement filtres',
    dateHeure: '2026-11-20T09:00:00Z',
    kilometrage: 24500,
  },
  {
    type: 'RESERVATION',
    description: 'Réservation LV-1001 · COMPLETED',
    dateHeure: '2026-11-10T14:00:00Z',
    nouveauStatut: 'COMPLETED',
  },
];

describe('VehicleHistoryModalComponent', () => {
  let component: VehicleHistoryModalComponent;
  let fixture: ComponentFixture<VehicleHistoryModalComponent>;
  let fleetServiceSpy: jasmine.SpyObj<FleetService>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<VehicleHistoryModalComponent>>;

  beforeEach(async () => {
    fleetServiceSpy = jasmine.createSpyObj('FleetService', ['getHistory']);
    fleetServiceSpy.getHistory.and.returnValue(of(MOCK_HISTORY));
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    const dialogData: VehicleHistoryModalData = { vehicle: MOCK_VEHICLE };

    await TestBed.configureTestingModule({
      imports: [VehicleHistoryModalComponent, NoopAnimationsModule],
      providers: [
        { provide: FleetService, useValue: fleetServiceSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleHistoryModalComponent);
    component = fixture.componentInstance;
  });

  it('devrait charger et afficher la timeline d’historique dans la modale', () => {
    fixture.detectChanges();

    expect(fleetServiceSpy.getHistory).toHaveBeenCalledWith(1);
    const items = fixture.nativeElement.querySelectorAll('[data-testid="history-timeline-item"]');
    expect(items.length).toBe(3);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dacia Duster');
    expect(text).toContain('Retour de révision');
    expect(text).toContain('Vidange + changement filtres');
    expect(text).toContain('24,500 km');
  });

  it('devrait filtrer les événements de la timeline par type', () => {
    fixture.detectChanges();

    component.setTypeFilter('MAINTENANCE');
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('[data-testid="history-timeline-item"]');
    expect(items.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Vidange');
  });

  it('devrait afficher un état vide lorsque l’historique est vide', () => {
    fleetServiceSpy.getHistory.and.returnValue(of([]));
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('[data-testid="history-empty"]');
    expect(empty).toBeTruthy();
  });

  it('devrait afficher un message d’erreur en cas d’échec API', () => {
    fleetServiceSpy.getHistory.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[data-testid="history-error"]');
    expect(errorEl).toBeTruthy();
  });

  it('devrait fermer la modale au clic sur Fermer', () => {
    fixture.detectChanges();
    component.close();
    expect(dialogRefSpy.close).toHaveBeenCalled();
  });
});
