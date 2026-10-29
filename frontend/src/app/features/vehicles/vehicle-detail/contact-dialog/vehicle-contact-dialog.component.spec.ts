import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';

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
    await TestBed.configureTestingModule({
      imports: [VehicleContactDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: mockData },
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

  it('should mark as submitted when valid form is submitted', fakeAsync(() => {
    component.contactForm.setValue({
      nom: 'Karim Idrissi',
      email: 'karim@example.com',
      telephone: '+212600112233',
      message: 'Je veux réserver pour ce weekend.',
    });

    expect(component.contactForm.valid).toBeTrue();
    component.onSubmit();
    expect(component.isSending).toBeTrue();

    tick(500);
    expect(component.isSending).toBeFalse();
    expect(component.isSubmitted).toBeTrue();
  }));

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
