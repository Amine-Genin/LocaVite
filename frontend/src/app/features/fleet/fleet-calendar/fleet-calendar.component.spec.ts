import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { FleetCalendarComponent } from './fleet-calendar.component';
import { FleetVehicle } from '../../vehicles/services/fleet.service';
import { Reservation } from '../../reservations/models/reservation.model';

function formatIsoOffset(offsetDays: number): string {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + offsetDays);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

const MOCK_VEHICLES: FleetVehicle[] = [
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
];

const MOCK_RESERVATIONS: Reservation[] = [
  {
    id: 501,
    code: 'LV-501',
    vehicleId: 1,
    clientNom: 'Karim Idrissi',
    dateDebut: formatIsoOffset(1),
    dateFin: formatIsoOffset(4),
    prixTotal: 1600,
    statut: 'CONFIRMED',
  },
  {
    id: 502,
    code: 'LV-502',
    vehicleId: 2,
    clientNom: 'Sara Alami',
    dateDebut: formatIsoOffset(2),
    dateFin: formatIsoOffset(5),
    prixTotal: 1200,
    statut: 'ACTIVE',
  },
];

describe('FleetCalendarComponent', () => {
  let component: FleetCalendarComponent;
  let fixture: ComponentFixture<FleetCalendarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FleetCalendarComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FleetCalendarComponent);
    component = fixture.componentInstance;
    component.vehicles = MOCK_VEHICLES;
    component.reservations = MOCK_RESERVATIONS;
    fixture.detectChanges();
  });

  it('devrait afficher les véhicules et les barres de réservation Gantt', () => {
    const rows = fixture.nativeElement.querySelectorAll('.gantt-row');
    expect(rows.length).toBe(2);

    const bar1 = fixture.nativeElement.querySelector('[data-testid="gantt-bar-501"]');
    const bar2 = fixture.nativeElement.querySelector('[data-testid="gantt-bar-502"]');
    expect(bar1).toBeTruthy();
    expect(bar2).toBeTruthy();
  });

  it('devrait filtrer sur un véhicule spécifique et permettre de réinitialiser', () => {
    const clearSpy = spyOn(component.clearVehicleFilter, 'emit');
    component.selectedVehicleId = 1;
    fixture.detectChanges();

    expect(component.visibleVehicles().length).toBe(1);
    const clearBtn = fixture.nativeElement.querySelector('[data-testid="calendar-clear-vehicle-btn"]');
    expect(clearBtn).toBeTruthy();
    clearBtn.click();
    expect(clearSpy).toHaveBeenCalled();
  });

  it('devrait changer la période affichée (7j / 14j / 30j) et naviguer', () => {
    expect(component.days().length).toBe(14);
    component.setRange(30);
    expect(component.days().length).toBe(30);

    const before = component.startDate().getTime();
    component.shiftPeriod(1);
    expect(component.startDate().getTime()).toBeGreaterThan(before);
  });
});
