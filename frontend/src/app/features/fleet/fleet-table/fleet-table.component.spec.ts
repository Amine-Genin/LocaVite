import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { FleetTableComponent } from './fleet-table.component';
import { FleetVehicle } from '../../vehicles/services/fleet.service';

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
    localisationActuelle: 'Aéroport Mohammed V',
    prochaineReservationId: 101,
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
];

describe('FleetTableComponent', () => {
  let component: FleetTableComponent;
  let fixture: ComponentFixture<FleetTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FleetTableComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FleetTableComponent);
    component = fixture.componentInstance;
  });

  it('devrait afficher les véhicules dans le tableau avec toutes les colonnes clés', () => {
    fixture.componentRef.setInput('vehicles', MOCK_VEHICLES);
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tr.fleet-row');
    expect(rows.length).toBe(2);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dacia Duster');
    expect(text).toContain('12345-A-1');
    expect(text).toContain('Aéroport Mohammed V');
    expect(text).toContain('78.5%');
    expect(text).toContain('Peugeot 208');
    expect(text).toContain('Rabat');
  });

  it('devrait afficher un état vide lorsqu’aucun véhicule n’est fourni', () => {
    fixture.componentRef.setInput('vehicles', []);
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('[data-testid="fleet-table-empty"]');
    expect(empty).toBeTruthy();
  });

  it('devrait émettre statusChange uniquement si le statut sélectionné est différent', () => {
    const spy = spyOn(component.statusChange, 'emit');
    component.onChangeStatus(MOCK_VEHICLES[0], 'PUBLISHED');
    expect(spy).not.toHaveBeenCalled();

    component.onChangeStatus(MOCK_VEHICLES[0], 'MAINTENANCE');
    expect(spy).toHaveBeenCalledWith({ vehicle: MOCK_VEHICLES[0], statut: 'MAINTENANCE' });
  });

  it('devrait calculer la classe CSS du taux d’occupation', () => {
    expect(component.getOccupancyClass(80)).toBe('occ-high');
    expect(component.getOccupancyClass(50)).toBe('occ-mid');
    expect(component.getOccupancyClass(15)).toBe('occ-low');
  });
});
