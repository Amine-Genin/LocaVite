import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { FleetService, FleetVehicle } from '../services/fleet.service';
import { AdminVehicleListPlaceholderComponent } from './admin-vehicle-list-placeholder.component';

describe('AdminVehicleListPlaceholderComponent', () => {
  let fixture: ComponentFixture<AdminVehicleListPlaceholderComponent>;
  let fleet: jasmine.SpyObj<FleetService>;
  const sample: FleetVehicle = {
    id: 21, matricule: '12345-A-1', marque: 'Dacia', modele: 'Duster', annee: 2024,
    categorie: 'SUV', statut: 'PUBLISHED', ville: 'Casablanca', localisationActuelle: 'Casablanca',
    kilometrage: 12000, revenusMois: 5400, revenusAnnee: 36000, tauxOccupation: 41, agenceId: 4,
  };

  beforeEach(async () => {
    fleet = jasmine.createSpyObj<FleetService>('FleetService', ['getFleet', 'updateStatus', 'getHistory', 'addMaintenance', 'addIncident', 'transfer']);
    fleet.getFleet.and.returnValue(of([sample]));
    fleet.getHistory.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [AdminVehicleListPlaceholderComponent],
      providers: [provideRouter([]), { provide: FleetService, useValue: fleet }, { provide: AuthService, useValue: { hasRole: () => false } }],
    }).compileComponents();
    fixture = TestBed.createComponent(AdminVehicleListPlaceholderComponent);
    fixture.detectChanges();
  });

  it('loads and displays the agency fleet row and key performance values', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(fleet.getFleet).toHaveBeenCalled();
    expect(el.textContent).toContain('Dacia Duster');
    expect(el.textContent).toContain('12345-A-1');
    expect(el.textContent).toContain('36,000');
    expect(el.querySelector('[data-cy="add-vehicle-nav-btn"]')?.getAttribute('href')).toContain('/admin/vehicles/new');
  });

  it('refreshes the fleet with selected filters', () => {
    const component = fixture.componentInstance;
    component.setFilter('ville', 'Rabat');
    component.applyFilters();
    expect(fleet.getFleet).toHaveBeenCalledWith({ statut: '', categorie: '', ville: 'Rabat' });
  });

  it('loads a vehicle history when the history action is selected', () => {
    fixture.componentInstance.showHistory(sample.id);
    expect(fleet.getHistory).toHaveBeenCalledWith(sample.id);
    expect(fixture.componentInstance.selectedVehicle()).toBe(sample.id);
  });
});
