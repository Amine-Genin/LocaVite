import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ReservationCardComponent } from './reservation-card.component';
import { Reservation } from '../models/reservation.model';

describe('ReservationCardComponent', () => {
  let component: ReservationCardComponent;
  let fixture: ComponentFixture<ReservationCardComponent>;

  const mockReservation: Reservation = {
    id: 12,
    code: 'LV-ABC123',
    dateDebut: '2026-11-20',
    dateFin: '2026-11-25',
    prixTotal: 1500,
    caution: 3000,
    statut: 'PENDING',
    vehicleId: 4,
    vehicle: {
      id: 4,
      marque: 'Renault',
      modele: 'Clio 5',
      annee: 2023,
      categorie: 'Citadine',
      ville: 'Casablanca',
      prixJour: 300,
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReservationCardComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ReservationCardComponent);
    component = fixture.componentInstance;
    component.reservation = { ...mockReservation };
    fixture.detectChanges();
  });

  it('should create and render reservation details', () => {
    expect(component).toBeTruthy();
    expect(component.vehicleTitle).toContain('Renault Clio 5');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('LV-ABC123');
    expect(compiled.textContent).toContain('Renault Clio 5');
  });

  it('should display correct status badge class and label for PENDING', () => {
    expect(component.statusClass).toBe('status-pending');
    expect(component.statusLabel).toBe('En attente');
    expect(component.canCancel).toBeTrue();
  });

  it('should emit cancel event on cancel button click', () => {
    spyOn(component.cancelReservation, 'emit');
    const cancelBtn = fixture.nativeElement.querySelector('[data-testid="btn-cancel-reservation"]');
    expect(cancelBtn).toBeTruthy();
    cancelBtn.click();
    expect(component.cancelReservation.emit).toHaveBeenCalledWith(component.reservation);
  });

  it('should emit viewDetails event', () => {
    spyOn(component.viewDetails, 'emit');
    const detailsBtn = fixture.nativeElement.querySelector('[data-testid="btn-view-details"]');
    detailsBtn.click();
    expect(component.viewDetails.emit).toHaveBeenCalledWith(component.reservation);
  });

  it('should allow download and extension for CONFIRMED status', () => {
    component.reservation = { ...mockReservation, statut: 'CONFIRMED' };
    fixture.detectChanges();

    expect(component.statusClass).toBe('status-confirmed');
    expect(component.canDownloadContract).toBeTrue();
    expect(component.canExtend).toBeTrue();
    expect(component.canCancel).toBeTrue();
  });
});
