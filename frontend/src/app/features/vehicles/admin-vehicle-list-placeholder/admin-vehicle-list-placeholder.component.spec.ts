import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { AdminVehicleListPlaceholderComponent } from './admin-vehicle-list-placeholder.component';

describe('AdminVehicleListPlaceholderComponent', () => {
  let fixture: ComponentFixture<AdminVehicleListPlaceholderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminVehicleListPlaceholderComponent, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminVehicleListPlaceholderComponent);
    fixture.detectChanges();
  });

  it('should render the admin fleet page with a link to create a vehicle', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-cy="admin-vehicles-page"]')).toBeTruthy();
    expect(el.querySelector('[data-cy="admin-vehicles-title"]')?.textContent).toContain(
      'Gestion de la flotte',
    );
    expect(el.querySelector('[data-cy="add-vehicle-nav-btn"]')?.getAttribute('href')).toContain(
      '/admin/vehicles/new',
    );
  });
});
