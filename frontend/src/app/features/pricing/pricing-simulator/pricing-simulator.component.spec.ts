import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { PricingSimulatorComponent } from './pricing-simulator.component';
import { PricingAdminService, PricingSimulateResponse } from '../services/pricing-admin.service';
import { VehicleService } from '../../vehicles/services/vehicle.service';

const MOCK_QUOTE: PricingSimulateResponse = {
  sousTotal: 2400,
  options: 200,
  remiseDuree: 0,
  remisePromo: 0,
  remiseTotale: 0,
  caution: 3000,
  tva: 520,
  total: 6120,
  jours: 4,
  promoCodeApplique: null,
};

describe('PricingSimulatorComponent', () => {
  let component: PricingSimulatorComponent;
  let fixture: ComponentFixture<PricingSimulatorComponent>;
  let pricingAdminSpy: jasmine.SpyObj<PricingAdminService>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;

  beforeEach(async () => {
    pricingAdminSpy = jasmine.createSpyObj('PricingAdminService', ['simulatePrice']);
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', ['search']);

    pricingAdminSpy.simulatePrice.and.returnValue(of(MOCK_QUOTE));
    vehicleServiceSpy.search.and.returnValue(
      of({
        content: [
          {
            id: 42,
            marque: 'Dacia',
            modele: 'Duster',
            annee: 2024,
            categorie: 'SUV',
            prixJour: 400,
            ville: 'Casablanca',
            statut: 'PUBLISHED',
          } as never,
        ],
        page: { size: 100, number: 0, totalElements: 1, totalPages: 1 },
        totalElements: 1,
        totalPages: 1,
        size: 100,
        number: 0,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [PricingSimulatorComponent, NoopAnimationsModule],
      providers: [
        { provide: PricingAdminService, useValue: pricingAdminSpy },
        { provide: VehicleService, useValue: vehicleServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PricingSimulatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait calculer et afficher le bon prix via PricingAdminService.simulatePrice', () => {
    component.toggleOption('GPS', true);
    fixture.detectChanges();

    expect(pricingAdminSpy.simulatePrice).toHaveBeenCalledWith(
      jasmine.objectContaining({
        vehicleId: 42,
        options: ['GPS'],
      }),
    );

    const totalEl = fixture.nativeElement.querySelector('[data-testid="sim-total"]');
    expect(totalEl.textContent).toContain('6,120.00 MAD');
    expect(component.result()?.sousTotal).toBe(2400);
    expect(component.result()?.options).toBe(200);
  });
});
