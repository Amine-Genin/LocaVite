import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { VehicleFormComponent } from './vehicle-form.component';
import { VehicleService } from '../services/vehicle.service';
import { Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { CatalogEntry, Vehicle, VehiclePhoto } from '../models/vehicle.model';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';

describe('VehicleFormComponent', () => {
  let component: VehicleFormComponent;
  let fixture: ComponentFixture<VehicleFormComponent>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;
  let router: Router;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const mockCreatedVehicle: Vehicle = {
    id: 42,
    marque: 'Renault',
    modele: 'Clio 5',
    annee: 2022,
    categorie: 'Économique',
    ville: 'Casablanca',
    prixJour: 350,
    prixVente: 140000,
    carburant: 'ESSENCE',
    transmission: 'MANUELLE',
    places: 5,
    description: 'Voiture comme neuve',
    matricule: '12345-A-1',
    statut: 'PUBLISHED',
    agenceId: 1,
    photos: [],
  };

  const mockUploadedPhotos: VehiclePhoto[] = [
    { id: 101, vehicleId: 42, url: '/photos/101.jpg', isPrimary: true, ordre: 0 },
    { id: 102, vehicleId: 42, url: '/photos/102.jpg', isPrimary: false, ordre: 1 },
  ];

  beforeEach(async () => {
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', [
      'getBrands',
      'getCategories',
      'create',
      'uploadPhotos',
      'setPrimaryPhoto',
    ]);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    vehicleServiceSpy.getBrands.and.returnValue(
      of([
        { id: 1, name: 'Renault' },
        { id: 2, name: 'Dacia' },
      ]),
    );
    vehicleServiceSpy.getCategories.and.returnValue(
      of([
        { id: 1, name: 'Économique' },
        { id: 2, name: 'SUV' },
      ]),
    );
    vehicleServiceSpy.create.and.returnValue(of(mockCreatedVehicle));
    vehicleServiceSpy.uploadPhotos.and.returnValue(of(mockUploadedPhotos));
    vehicleServiceSpy.setPrimaryPhoto.and.returnValue(of(mockUploadedPhotos[1]));

    await TestBed.configureTestingModule({
      imports: [VehicleFormComponent, TranslateModule.forRoot(), NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: VehicleService, useValue: vehicleServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(VehicleFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the vehicle form component', () => {
    expect(component).toBeTruthy();
  });

  it('should load brands and categories on init', () => {
    expect(vehicleServiceSpy.getBrands).toHaveBeenCalled();
    expect(vehicleServiceSpy.getCategories).toHaveBeenCalled();
    expect(component.brands.length).toBe(2);
    expect(component.categories.length).toBe(2);
  });

  it('formulaire invalide -> bouton désactivé', () => {
    // Initial form is missing brandId, modele, matricule, etc.
    expect(component.form.valid).toBeFalse();

    const publishBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-cy="publish-button"]',
    );
    const draftBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-cy="draft-button"]',
    );

    expect(publishBtn.disabled).toBeTrue();
    expect(draftBtn.disabled).toBeTrue();
  });

  it('should filter brands autocomplete list based on input', fakeAsync(() => {
    let filtered: CatalogEntry[] = [];
    component.filteredBrands$.subscribe((list) => (filtered = list));

    component.brandSearchControl.setValue('ren');
    tick();

    expect(filtered.length).toBe(1);
    expect(filtered[0].name).toBe('Renault');
  }));

  it('should sync brandId when brand is selected from autocomplete', () => {
    const selectedBrand = { id: 1, name: 'Renault' };
    component.onBrandSelected({
      option: { value: selectedBrand },
    } as MatAutocompleteSelectedEvent);

    expect(component.form.get('brandId')?.value).toBe(1);
    expect(component.brandSearchControl.value).toBe('Renault');
  });

  it('should enable publish and draft buttons when form is valid', () => {
    component.form.patchValue({
      brandId: 1,
      modele: 'Clio 5',
      annee: 2022,
      categoryId: 1,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      description: 'Superbe état, climatisée',
      prixJour: 350,
      prixVente: 140000,
      ville: 'Casablanca',
    });
    fixture.detectChanges();

    expect(component.form.valid).toBeTrue();

    const publishBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-cy="publish-button"]',
    );
    const draftBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-cy="draft-button"]',
    );

    expect(publishBtn.disabled).toBeFalse();
    expect(draftBtn.disabled).toBeFalse();
  });

  it('should submit vehicle with statut PUBLISHED and redirect to /admin/vehicles', () => {
    component.form.patchValue({
      brandId: 1,
      modele: 'Clio 5',
      annee: 2022,
      categoryId: 1,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      description: 'Superbe état',
      prixJour: 350,
      prixVente: 140000,
      ville: 'Casablanca',
    });

    component.onSubmit('PUBLISHED');

    expect(vehicleServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        brandId: 1,
        modele: 'Clio 5',
        statut: 'PUBLISHED',
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/admin/vehicles']);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Véhicule publié avec succès',
      'Fermer',
      jasmine.any(Object),
    );
  });

  it('should submit vehicle with statut DRAFT', () => {
    component.form.patchValue({
      brandId: 2,
      modele: 'Duster',
      annee: 2023,
      categoryId: 2,
      carburant: 'DIESEL',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '67890-B-2',
      description: '4x4',
      prixJour: 400,
      prixVente: 180000,
      ville: 'Rabat',
    });

    component.onSubmit('DRAFT');

    expect(vehicleServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({
        brandId: 2,
        modele: 'Duster',
        statut: 'DRAFT',
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/admin/vehicles']);
  });

  it('should upload photos and set primary photo if photos are attached', () => {
    const dummyFile1 = new File(['f1'], 'p1.jpg', { type: 'image/jpeg' });
    const dummyFile2 = new File(['f2'], 'p2.jpg', { type: 'image/jpeg' });

    component.form.patchValue({
      brandId: 1,
      modele: 'Clio 5',
      annee: 2022,
      categoryId: 1,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      description: 'Superbe',
      prixJour: 350,
      prixVente: 140000,
      ville: 'Casablanca',
    });

    // Second photo is primary
    component.photos = [
      {
        file: dummyFile1,
        previewUrl: 'blob:1',
        name: 'p1.jpg',
        size: 100,
        isPrimary: false,
        ordre: 0,
      },
      {
        file: dummyFile2,
        previewUrl: 'blob:2',
        name: 'p2.jpg',
        size: 100,
        isPrimary: true,
        ordre: 1,
      },
    ];

    component.onSubmit('PUBLISHED');

    expect(vehicleServiceSpy.uploadPhotos).toHaveBeenCalledWith(42, [dummyFile1, dummyFile2]);
    expect(vehicleServiceSpy.setPrimaryPhoto).toHaveBeenCalledWith(42, 102);
    expect(router.navigate).toHaveBeenCalledWith(['/admin/vehicles']);
  });

  it('should handle backend error gracefully', () => {
    vehicleServiceSpy.create.and.returnValue(
      throwError(() => ({ error: { message: 'Le matricule existe déjà.' } })),
    );

    component.form.patchValue({
      brandId: 1,
      modele: 'Clio 5',
      annee: 2022,
      categoryId: 1,
      carburant: 'ESSENCE',
      transmission: 'MANUELLE',
      places: 5,
      matricule: '12345-A-1',
      description: 'Superbe',
      prixJour: 350,
      prixVente: 140000,
      ville: 'Casablanca',
    });

    component.onSubmit('PUBLISHED');
    fixture.detectChanges();

    expect(component.errorMessage).toBe('Le matricule existe déjà.');
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Le matricule existe déjà.',
      'Fermer',
      jasmine.any(Object),
    );
  });
});
