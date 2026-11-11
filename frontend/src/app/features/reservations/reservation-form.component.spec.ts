import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepper } from '@angular/material/stepper';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { ReservationFormComponent } from './reservation-form.component';
import { ReservationService } from './services/reservation.service';
import { VehicleService } from '../vehicles/services/vehicle.service';
import { UserService } from '../profile/services/user.service';
import { Vehicle } from '../vehicles/models/vehicle.model';
import { User } from '../profile/models/user.model';
import { PriceQuote, Reservation } from './models/reservation.model';

describe('ReservationFormComponent', () => {
  let component: ReservationFormComponent;
  let fixture: ComponentFixture<ReservationFormComponent>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const mockVehicle: Vehicle = {
    id: 1,
    marque: 'Dacia',
    modele: 'Duster',
    annee: 2024,
    categorie: 'SUV',
    ville: 'Casablanca',
    prixJour: 350,
    prixVente: 195000,
    carburant: 'DIESEL',
    transmission: 'MANUELLE',
    places: 5,
    description: 'SUV compact économique',
    matricule: '78945-A-1',
    statut: 'PUBLISHED',
    agenceId: 10,
    photos: [],
  };

  const mockValidatedUser: User = {
    id: 2,
    firstName: 'Ahmed',
    lastName: 'Benali',
    email: 'ahmed.benali@locavite.ma',
    phone: '0612345678',
    role: 'CLIENT',
    createdAt: '2026-01-15T10:00:00',
    documents: [
      {
        id: 10,
        type: 'permis',
        url: '/docs/permis.pdf',
        status: 'validated',
        uploadedAt: '2026-01-16T10:00:00',
      },
      {
        id: 11,
        type: 'cin',
        url: '/docs/cin.pdf',
        status: 'validated',
        uploadedAt: '2026-01-16T11:00:00',
      },
    ],
  };

  const mockQuoteBase: PriceQuote = {
    sousTotal: 1400,
    options: 0,
    remise: 0,
    caution: 3000,
    tva: 280,
    total: 4680,
    jours: 4,
  };

  const mockQuoteWithOptions: PriceQuote = {
    sousTotal: 1400,
    options: 320,
    remise: 0,
    caution: 3000,
    tva: 344,
    total: 5064,
    jours: 4,
  };

  const mockCreatedReservation: Reservation = {
    id: 77,
    code: 'LV-A1B2C3',
    statut: 'PENDING',
    prixTotal: 5064,
  };

  const futureDate = (daysFromToday: number): Date => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() + daysFromToday);
    return d;
  };

  beforeEach(async () => {
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', ['getById']);
    reservationServiceSpy = jasmine.createSpyObj('ReservationService', [
      'getQuote',
      'checkAvailability',
      'create',
    ]);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getProfile']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    vehicleServiceSpy.getById.and.returnValue(of(mockVehicle));
    userServiceSpy.getProfile.and.returnValue(of(mockValidatedUser));
    reservationServiceSpy.checkAvailability.and.returnValue(
      of({ available: [{ from: '2026-11-15', to: '2026-11-19' }], unavailable: [] }),
    );
    reservationServiceSpy.getQuote.and.returnValue(of(mockQuoteBase));
    reservationServiceSpy.create.and.returnValue(of(mockCreatedReservation));

    await TestBed.configureTestingModule({
      imports: [ReservationFormComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ vehicleId: '1' }),
            },
          },
        },
        { provide: VehicleService, useValue: vehicleServiceSpy },
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(ReservationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant et charger le véhicule ainsi que les documents du client', () => {
    expect(component).toBeTruthy();
    expect(vehicleServiceSpy.getById).toHaveBeenCalledWith(1);
    expect(userServiceSpy.getProfile).toHaveBeenCalled();
    expect(component.vehicle()).toEqual(mockVehicle);
    expect(component.documentsValid()).toBeTrue();
  });

  describe('Validation des dates', () => {
    it('devrait invalider le formulaire si les dates sont vides', () => {
      expect(component.datesForm.valid).toBeFalse();
      expect(component.datesForm.controls.dateDebut.hasError('required')).toBeTrue();
      expect(component.datesForm.controls.dateFin.hasError('required')).toBeTrue();
    });

    it("devrait rejeter une date de début antérieure à aujourd'hui (min aujourd'hui)", () => {
      const yesterday = futureDate(-1);
      const tomorrow = futureDate(2);

      component.datesForm.setValue({ dateDebut: yesterday, dateFin: tomorrow });
      expect(component.datesForm.controls.dateDebut.hasError('minDate')).toBeTrue();
      expect(component.datesForm.valid).toBeFalse();
    });

    it('devrait rejeter une date de fin antérieure ou égale à la date de début (cohérence)', () => {
      const start = futureDate(3);
      const sameDay = futureDate(3);
      const beforeStart = futureDate(2);

      component.datesForm.setValue({ dateDebut: start, dateFin: sameDay });
      expect(component.datesForm.hasError('invalidDateRange')).toBeTrue();
      expect(component.datesForm.valid).toBeFalse();

      component.datesForm.setValue({ dateDebut: start, dateFin: beforeStart });
      expect(component.datesForm.hasError('invalidDateRange')).toBeTrue();
      expect(component.datesForm.valid).toBeFalse();
    });

    it("devrait valider une plage de dates cohérente à partir d'aujourd'hui", () => {
      const start = futureDate(0);
      const end = futureDate(4);

      component.datesForm.setValue({ dateDebut: start, dateFin: end });
      expect(component.datesForm.valid).toBeTrue();
      expect(component.datesForm.errors).toBeNull();
    });
  });

  describe('Calcul du prix temps réel (debounce 500ms)', () => {
    it('devrait calculer le devis et vérifier la disponibilité 500ms après changement de dates', fakeAsync(() => {
      const start = futureDate(2);
      const end = futureDate(6);

      component.datesForm.setValue({ dateDebut: start, dateFin: end });
      expect(reservationServiceSpy.getQuote).not.toHaveBeenCalled();

      tick(499);
      expect(reservationServiceSpy.getQuote).not.toHaveBeenCalled();

      tick(1);
      expect(reservationServiceSpy.checkAvailability).toHaveBeenCalled();
      expect(reservationServiceSpy.getQuote).toHaveBeenCalledWith(
        jasmine.objectContaining({
          vehicleId: 1,
          options: [],
        }),
      );
      expect(component.quote()).toEqual(mockQuoteBase);
      expect(component.available()).toBeTrue();

      fixture.detectChanges();
      const liveBanner = fixture.nativeElement.querySelector('[data-cy="realtime-price-total"]');
      expect(liveBanner?.textContent).toContain('4680.00 MAD');
      flush();
    }));

    it("devrait recalculer le prix en temps réel lorsqu'une option est cochée", fakeAsync(() => {
      const start = futureDate(2);
      const end = futureDate(6);

      component.datesForm.setValue({ dateDebut: start, dateFin: end });
      tick(500);
      expect(component.quote()?.total).toBe(4680);

      reservationServiceSpy.getQuote.and.returnValue(of(mockQuoteWithOptions));
      component.optionsForm.patchValue({ gps: true, babySeat: true });
      tick(500);

      expect(reservationServiceSpy.getQuote).toHaveBeenCalledWith(
        jasmine.objectContaining({
          vehicleId: 1,
          options: ['GPS', 'BABY_SEAT'],
        }),
      );
      expect(component.quote()).toEqual(mockQuoteWithOptions);

      fixture.detectChanges();
      const liveBanner = fixture.nativeElement.querySelector('[data-cy="realtime-price-total"]');
      expect(liveBanner?.textContent).toContain('5064.00 MAD');
      flush();
    }));
  });

  describe('Navigation entre étapes (MatStepper)', () => {
    it('devrait naviguer à travers les 5 étapes du MatStepper et soumettre la réservation', fakeAsync(() => {
      const stepperDebug = fixture.debugElement.query(By.directive(MatStepper));
      const stepper = stepperDebug.componentInstance as MatStepper;
      expect(stepper.selectedIndex).toBe(0);

      // Étape 1 : Dates
      component.datesForm.setValue({ dateDebut: futureDate(1), dateFin: futureDate(5) });
      tick(500);
      fixture.detectChanges();

      expect(component.datesForm.valid).toBeTrue();
      expect(component.available()).toBeTrue();

      stepper.next();
      fixture.detectChanges();
      expect(stepper.selectedIndex).toBe(1);

      // Étape 2 : Lieux (sélection radio + saisie)
      component.selectPickupType('Aéroport');
      component.placesForm.controls.lieuRetour.setValue('Agence - Casablanca');
      fixture.detectChanges();
      expect(component.placesForm.valid).toBeTrue();
      expect(component.placesForm.value.lieuPriseEnCharge).toBe('Aéroport - Casablanca');

      stepper.next();
      fixture.detectChanges();
      expect(stepper.selectedIndex).toBe(2);

      // Étape 3 : Options
      reservationServiceSpy.getQuote.and.returnValue(of(mockQuoteWithOptions));
      component.optionsForm.patchValue({ gps: true, extraDriver: true });
      tick(500);
      fixture.detectChanges();

      stepper.next();
      fixture.detectChanges();
      expect(stepper.selectedIndex).toBe(3);

      // Étape 4 : Récapitulatif
      const summaryTotal = fixture.nativeElement.querySelector('[data-cy="price-total"]');
      expect(summaryTotal?.textContent).toContain('5064.00 MAD');

      stepper.next();
      fixture.detectChanges();
      expect(stepper.selectedIndex).toBe(4);

      // Étape 5 : Confirmation (CGV + bouton Confirmer)
      expect(component.confirmationForm.valid).toBeFalse();
      component.confirmationForm.controls.acceptedTerms.setValue(true);
      fixture.detectChanges();
      expect(component.confirmationForm.valid).toBeTrue();

      component.submit();
      expect(reservationServiceSpy.create).toHaveBeenCalledWith(
        jasmine.objectContaining({
          vehicleId: 1,
          lieuPriseEnCharge: 'Aéroport - Casablanca',
          lieuRetour: 'Agence - Casablanca',
          options: ['GPS', 'EXTRA_DRIVER'],
        }),
      );
      expect(snackBarSpy.open).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/reservations', 77]);
      flush();
    }));

    it('devrait afficher une alerte et un lien vers /profile si les documents client ne sont pas validés', () => {
      userServiceSpy.getProfile.and.returnValue(
        of({
          ...mockValidatedUser,
          documents: [
            {
              id: 10,
              type: 'permis',
              url: '/docs/permis.pdf',
              status: 'pending',
              uploadedAt: '2026-01-16T10:00:00',
            },
          ],
        }),
      );

      component.ngOnInit();
      fixture.detectChanges();

      expect(component.documentsValid()).toBeFalse();
      const warningAlert = fixture.nativeElement.querySelector('[data-cy="documents-warning"]');
      const uploadLink = fixture.nativeElement.querySelector('[data-cy="upload-documents-link"]');
      expect(warningAlert).toBeTruthy();
      expect(uploadLink?.getAttribute('href')).toBe('/profile');
    });

    it('devrait gérer une erreur lors de la vérification de disponibilité', fakeAsync(() => {
      reservationServiceSpy.checkAvailability.and.returnValue(
        throwError(() => new Error('Network error')),
      );

      component.datesForm.setValue({ dateDebut: futureDate(1), dateFin: futureDate(3) });
      tick(500);

      expect(component.available()).toBeNull();
      expect(snackBarSpy.open).toHaveBeenCalled();
      flush();
    }));
  });
});
