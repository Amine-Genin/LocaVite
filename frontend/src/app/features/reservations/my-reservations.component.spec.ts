import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';

import { MyReservationsComponent } from './my-reservations.component';
import { ReservationService } from './services/reservation.service';
import { VehicleService } from '../vehicles/services/vehicle.service';
import { Reservation, ReservationPageResponse } from './models/reservation.model';

describe('MyReservationsComponent', () => {
  let component: MyReservationsComponent;
  let fixture: ComponentFixture<MyReservationsComponent>;
  let reservationServiceSpy: jasmine.SpyObj<ReservationService>;
  let vehicleServiceSpy: jasmine.SpyObj<VehicleService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let router: Router;

  const mockReservations: Reservation[] = [
    {
      id: 1,
      code: 'LV-UPCOMING1',
      statut: 'PENDING',
      dateDebut: '2026-11-20',
      dateFin: '2026-11-25',
      prixTotal: 1200,
      vehicleId: 10,
    },
    {
      id: 2,
      code: 'LV-ACTIVE1',
      statut: 'ACTIVE',
      dateDebut: '2026-11-10',
      dateFin: '2026-11-15',
      prixTotal: 2500,
      vehicleId: 11,
    },
    {
      id: 3,
      code: 'LV-COMPLETED1',
      statut: 'COMPLETED',
      dateDebut: '2026-10-01',
      dateFin: '2026-10-05',
      prixTotal: 900,
      vehicleId: 10,
    },
    {
      id: 4,
      code: 'LV-CANCELLED1',
      statut: 'CANCELLED',
      dateDebut: '2026-10-10',
      dateFin: '2026-10-12',
      prixTotal: 600,
      vehicleId: 12,
    },
  ];

  const mockPageResponse: ReservationPageResponse = {
    content: mockReservations,
    totalElements: 4,
    totalPages: 1,
    size: 20,
    number: 0,
  };

  beforeEach(async () => {
    reservationServiceSpy = jasmine.createSpyObj('ReservationService', [
      'getMyReservations',
      'cancel',
      'downloadContract',
    ]);
    vehicleServiceSpy = jasmine.createSpyObj('VehicleService', ['getVehicleById']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    reservationServiceSpy.getMyReservations.and.returnValue(of(mockPageResponse));
    vehicleServiceSpy.getVehicleById.and.returnValue(
      of({
        id: 10,
        marque: 'Peugeot',
        modele: '208',
        annee: 2022,
        categorie: 'Citadine',
        ville: 'Rabat',
        prixJour: 250,
      }),
    );

    await TestBed.configureTestingModule({
      imports: [MyReservationsComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ReservationService, useValue: reservationServiceSpy },
        { provide: VehicleService, useValue: vehicleServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: MatDialog, useValue: dialogSpy },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(MyReservationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load reservations on init', () => {
    expect(component).toBeTruthy();
    expect(reservationServiceSpy.getMyReservations).toHaveBeenCalled();
    expect(component.allReservations().length).toBe(4);
  });

  it('should filter reservations by tabs', () => {
    const upcoming = component.getFilteredReservations('upcoming');
    expect(upcoming.length).toBe(1);
    expect(upcoming[0].code).toBe('LV-UPCOMING1');

    const active = component.getFilteredReservations('active');
    expect(active.length).toBe(1);
    expect(active[0].code).toBe('LV-ACTIVE1');

    const completed = component.getFilteredReservations('completed');
    expect(completed.length).toBe(1);
    expect(completed[0].code).toBe('LV-COMPLETED1');

    const cancelled = component.getFilteredReservations('cancelled');
    expect(cancelled.length).toBe(1);
    expect(cancelled[0].code).toBe('LV-CANCELLED1');
  });

  it('should change current tab and reset pagination', () => {
    component.onTabChange(1); // Active
    expect(component.currentTab()).toBe('active');
    expect(component.pageIndex()).toBe(0);
  });

  it('should cancel reservation when confirmed', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    reservationServiceSpy.cancel.and.returnValue(of(undefined as void));

    const target = mockReservations[0];
    component.onCancel(target);

    expect(window.confirm).toHaveBeenCalled();
    expect(reservationServiceSpy.cancel).toHaveBeenCalledWith(target.id);
    expect(snackBarSpy.open).toHaveBeenCalledWith('Réservation annulée avec succès.', 'OK', {
      duration: 3000,
    });
  });

  it('should not cancel if user aborts confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const target = mockReservations[0];
    component.onCancel(target);

    expect(reservationServiceSpy.cancel).not.toHaveBeenCalled();
  });

  it('should navigate to reservation details', () => {
    component.onViewDetails(mockReservations[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/reservations', 1]);
  });

  it('should display empty state if no reservations match tab', () => {
    component.allReservations.set([]);
    fixture.detectChanges();

    const emptyElem = fixture.nativeElement.querySelector('[data-testid="empty-reservations"]');
    expect(emptyElem).toBeTruthy();
  });
});
