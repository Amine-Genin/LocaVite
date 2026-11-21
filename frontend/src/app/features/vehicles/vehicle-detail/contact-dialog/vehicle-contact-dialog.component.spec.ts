import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SaleListingContactService } from '../../services/sale-listing-contact.service';
import {
  VehicleContactDialogComponent,
  VehicleContactDialogData,
} from './vehicle-contact-dialog.component';

describe('VehicleContactDialogComponent', () => {
  let component: VehicleContactDialogComponent;
  let fixture: ComponentFixture<VehicleContactDialogComponent>;

  const mockDialogRef = {
    close: jasmine.createSpy('close'),
  };
  const mockContactService = jasmine.createSpyObj<SaleListingContactService>(
    'SaleListingContactService',
    ['getPublishedListingIdByVehicle', 'sendContactRequest'],
  );
  const mockAuthService = jasmine.createSpyObj<AuthService>('AuthService', ['getCurrentUser']);

  const mockData: VehicleContactDialogData = {
    vehicle: {
      id: 1,
      marque: 'Dacia',
      modele: 'Duster',
      annee: 2024,
      categorie: 'SUV',
      ville: 'Casablanca',
      prixJour: 350,
      agenceTelephone: '+212612345678',
    },
  };

  beforeEach(async () => {
    mockContactService.getPublishedListingIdByVehicle.and.returnValue(of(42));
    mockContactService.sendContactRequest.and.returnValue(of({ id: 5, saleListingId: 42 }));
    mockAuthService.getCurrentUser.and.returnValue(null);
    mockContactService.getPublishedListingIdByVehicle.calls.reset();
    mockContactService.sendContactRequest.calls.reset();
    mockAuthService.getCurrentUser.calls.reset();
    await TestBed.configureTestingModule({
      imports: [VehicleContactDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: mockData },
        { provide: SaleListingContactService, useValue: mockContactService },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VehicleContactDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the contact dialog', () => {
    expect(component).toBeTruthy();
    expect(component.vehicle.marque).toBe('Dacia');
    expect(component.contactForm.get('message')?.value).toContain('Dacia Duster');
  });

  it('should prefill contact details for a signed-in user', () => {
    mockAuthService.getCurrentUser.and.returnValue({
      id: 9,
      nom: 'Idrissi',
      prenom: 'Karim',
      email: 'karim@example.com',
      telephone: '+212600112233',
      roles: ['CLIENT'],
      createdAt: '2026-01-01',
    });

    const signedInFixture = TestBed.createComponent(VehicleContactDialogComponent);
    const signedInComponent = signedInFixture.componentInstance;

    expect(signedInComponent.contactForm.value).toEqual({
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: signedInComponent.contactForm.get('message')?.value,
    });
    signedInFixture.destroy();
  });

  it('should validate form and prevent submission if fields are empty', () => {
    component.contactForm.setValue({
      nom: '',
      email: '',
      telephone: '',
      message: '',
    });

    component.onSubmit();
    expect(component.isSubmitted).toBeFalse();
    expect(component.contactForm.valid).toBeFalse();
  });

  it('should send a valid contact request to the matching sale listing', () => {
    component.contactForm.setValue({
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: 'Je veux réserver pour ce weekend.',
    });

    expect(component.contactForm.valid).toBeTrue();
    component.onSubmit();
    expect(component.isSending).toBeFalse();
    expect(component.isSubmitted).toBeTrue();
    expect(mockContactService.sendContactRequest).toHaveBeenCalledWith(42, {
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: 'Je veux réserver pour ce weekend.',
    });
  });

  it('should keep the form open when the API rejects the request', () => {
    mockContactService.sendContactRequest.and.returnValue(
      throwError(() => new Error('Request failed')),
    );
    component.contactForm.setValue({
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: 'Je veux réserver pour ce weekend.',
    });

    component.onSubmit();

    expect(component.isSubmitted).toBeFalse();
    expect(component.submitFailed).toBeTrue();
  });

  it('should construct correct WhatsApp URL', () => {
    const url = component.whatsappUrl;
    expect(url).toContain('wa.me/212612345678');
    expect(url).toContain(encodeURIComponent('Dacia'));
  });

  it('should call window.open on openWhatsApp', () => {
    spyOn(window, 'open');
    component.openWhatsApp();
    expect(window.open).toHaveBeenCalledWith(
      component.whatsappUrl,
      '_blank',
      'noopener,noreferrer',
    );
  });

  it('should close dialog on close()', () => {
    component.close();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });
});
