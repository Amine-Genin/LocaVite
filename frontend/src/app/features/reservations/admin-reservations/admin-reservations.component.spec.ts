import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AdminReservationsComponent } from './admin-reservations.component';
import { ReservationService } from '../services/reservation.service';
import { VehicleService } from '../../vehicles/services/vehicle.service';
import { Reservation, ReservationPageResponse } from '../models/reservation.model';

describe('AdminReservationsComponent', () => {
  let component: AdminReservationsComponent;
  let fixture: ComponentFixture<AdminReservationsComponent>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  const mockReservations: Reservation[] = [
    {
      id: 1,
      code: 'LV-TEST01',
      statut: 'PENDING',
      dateDebut: '2026-11-20',
      dateFin: '2026-11-25',
      prixTotal: 1500,
      caution: 3000,
      vehicleId: 10,
      clientNom: 'Ahmed Benali',
    },
    {
      id: 2,
      code: 'LV-TEST02',
      statut: 'ACTIVE',
      dateDebut: '2026-11-10',
      dateFin: '2026-11-15',
      prixTotal: 2500,
      caution: 5000,
      vehicleId: 11,
      clientNom: 'Sara Mansouri',
    },
  ];

  const mockPageResponse: ReservationPageResponse = {
    content: mockReservations,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(async () => {
    reservationServiceSpy = jasmine.createSpyObj('ReservationService', [
      'getAll',
      'confirm',
      'complete',
      'cancel',
      'downloadContract',
    ]);
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', ['getVehicleById']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    reservationServiceSpy.getAll.and.returnValue(of(mockPageResponse));
    vehicleServiceSpy.getVehicleById.and.returnValue(
      of({
        id: 10,
        marque: 'Dacia',
        modele: 'Duster',
        annee: 2023,
        categorie: 'SUV',
        ville: 'Casablanca',
        prixJour: 400,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [AdminReservationsComponent, NoopAnimationsModule],
      providers: [
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: VehicleService, useValue: vehicleServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReservationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load reservations', () => {
    expect(component).toBeTruthy();
    expect(reservationServiceSpy.getAll).toHaveBeenCalled();
    expect(component.reservations().length).toBe(2);
  });

  it('should calculate statistics properly', () => {
    const stats = component.stats();
    expect(stats.totalCount).toBe(2);
    expect(stats.pendingCount).toBe(1);
    expect(stats.activeCount).toBe(1);
    expect(stats.monthlyRevenue).toBe(4000); // 1500 + 2500
  });

  it('should call confirm API when confirming a reservation', () => {
    reservationServiceSpy.confirm.and.returnValue(of(mockReservations[0]));
    component.onConfirm(mockReservations[0]);

    expect(reservationServiceSpy.confirm).toHaveBeenCalledWith(1);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Réservation LV-TEST01 confirmée avec succès.',
      'OK',
      { duration: 3000 },
    );
  });

  it('should call complete API when completing a reservation', () => {
    reservationServiceSpy.complete.and.returnValue(of(mockReservations[1]));
    component.onComplete(mockReservations[1]);

    expect(reservationServiceSpy.complete).toHaveBeenCalledWith(2);
    expect(snackBarSpy.open).toHaveBeenCalledWith('Réservation LV-TEST02 clôturée.', 'OK', {
      duration: 3000,
    });
  });

  it('should filter reservations when submitting filter form', () => {
    component.filterForm.patchValue({ code: 'LV-TEST01' });
    component.onFilterSubmit();

    expect(reservationServiceSpy.getAll).toHaveBeenCalledWith(
      jasmine.objectContaining({
        code: 'LV-TEST01',
      }),
    );
  });
});
