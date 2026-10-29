import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { DEFAULT_VEHICLE_PHOTO, VehicleCardComponent } from './vehicle-card.component';
import { Vehicle } from '../models/vehicle.model';
import { setupFallbackTranslations } from '../vehicles.constants';

describe('VehicleCardComponent', () => {
  let component: VehicleCardComponent;
  let fixture: ComponentFixture<VehicleCardComponent>;

  const mockVehicle: Vehicle = {
    id: 42,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2024,
    categorie: 'SUV',
    ville: 'Casablanca',
    prixJour: 350,
    photoUrl: 'https://example.com/duster.jpg',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VehicleCardComponent, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleCardComponent);
    component = fixture.componentInstance;
    component.vehicle = { ...mockVehicle };
    fixture.detectChanges();
  });

  it('should create and display vehicle details (photo, brand, model, year, price/day, city, category, Details button)', () => {
    expect(component).toBeTruthy();
    const el = fixture.nativeElement as HTMLElement;

    const img = el.querySelector<HTMLImageElement>('[data-cy="vehicle-photo"]');
    expect(img).toBeTruthy();
    expect(img?.getAttribute('src')).toBe('https://example.com/duster.jpg');
    expect(img?.getAttribute('loading')).toBe('lazy');

    expect(el.querySelector('[data-cy="vehicle-brand"]')?.textContent).toContain('Dacia');
    expect(el.querySelector('[data-cy="vehicle-model"]')?.textContent).toContain('Duster');
    expect(el.querySelector('[data-cy="vehicle-year"]')?.textContent).toContain('2024');
    expect(el.querySelector('[data-cy="vehicle-city"]')?.textContent).toContain('Casablanca');
    expect(el.querySelector('[data-cy="vehicle-category"]')?.textContent).toContain('SUV');
    expect(el.querySelector('[data-cy="vehicle-price"]')?.textContent).toContain('350');
    expect(el.querySelector('[data-cy="details-btn"]')?.textContent?.trim()).toBe('Détails');
  });

  it('should fallback to placeholder image when photoUrl is missing or errors', () => {
    component.vehicle = { ...mockVehicle, photoUrl: null };
    component.ngOnChanges();
    fixture.detectChanges();

    expect(component.photoSrc).toBe(DEFAULT_VEHICLE_PHOTO);

    component.onImageError();
    expect(component.imageBroken).toBeTrue();
    expect(component.photoSrc).toBe(DEFAULT_VEHICLE_PHOTO);
  });

  it('should toggle favorite button (heart) and emit favoriteToggle event', () => {
    spyOn(component.favoriteToggle, 'emit');
    const favBtn = fixture.nativeElement.querySelector(
      '[data-cy="favorite-btn"]',
    ) as HTMLButtonElement;

    expect(favBtn.classList.contains('active')).toBeFalse();

    favBtn.click();
    fixture.detectChanges();

    expect(favBtn.classList.contains('active')).toBeTrue();
    expect(component.favoriteToggle.emit).toHaveBeenCalledWith(component.vehicle);
  });

  it('should emit detailsClick event when Details button is clicked', () => {
    spyOn(component.detailsClick, 'emit');
    const detailsBtn = fixture.nativeElement.querySelector(
      '[data-cy="details-btn"]',
    ) as HTMLButtonElement;

    detailsBtn.click();
    expect(component.detailsClick.emit).toHaveBeenCalledWith(component.vehicle);
  });

  it('should respect isFavorite input override', () => {
    component.isFavorite = true;
    fixture.detectChanges();
    expect(component.favoriteState).toBeTrue();

    component.isFavorite = false;
    fixture.detectChanges();
    expect(component.favoriteState).toBeFalse();
  });

  it('should safely handle setupFallbackTranslations with null/undefined translate service', () => {
    expect(() => setupFallbackTranslations(null)).not.toThrow();
    expect(() => setupFallbackTranslations(undefined)).not.toThrow();
  });
});
