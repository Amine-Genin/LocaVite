import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Vehicle } from '../../models/vehicle.model';
import { setupFallbackTranslations } from '../../vehicles.constants';

export interface VehicleContactDialogData {
  vehicle: Vehicle;
}

@Component({
  selector: 'app-vehicle-contact-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    TranslateModule,
  ],
  templateUrl: './vehicle-contact-dialog.component.html',
  styleUrl: './vehicle-contact-dialog.component.scss',
})
export class VehicleContactDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<VehicleContactDialogComponent>);
  private readonly translate = inject(TranslateService, { optional: true });

  contactForm: FormGroup;
  isSending = false;
  isSubmitted = false;

  readonly vehicle: Vehicle;
  readonly defaultPhone = '+212600000000';

  constructor(@Inject(MAT_DIALOG_DATA) public data: VehicleContactDialogData) {
    setupFallbackTranslations(this.translate);
    this.vehicle = data.vehicle;

    const defaultMsg = `Bonjour, je suis intéressé(e) par le véhicule ${this.vehicle.marque} ${this.vehicle.modele} (${this.vehicle.annee}, réf: #${this.vehicle.id}). Est-il toujours disponible ?`;

    this.contactForm = this.fb.group({
      nom: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.required]],
      message: [defaultMsg, [Validators.required]],
    });
  }

  get agencyPhoneNumber(): string {
    return this.vehicle.agenceTelephone || this.defaultPhone;
  }

  get whatsappUrl(): string {
    const rawNumber = this.agencyPhoneNumber.replace(/\D/g, '');
    const cleanPhone = rawNumber.startsWith('0') ? '212' + rawNumber.substring(1) : rawNumber;
    const text = encodeURIComponent(
      `Bonjour, je vous contacte depuis LocaVite à propos du véhicule ${this.vehicle.marque} ${this.vehicle.modele} (${this.vehicle.annee}, ID: ${this.vehicle.id}).`,
    );
    return `https://wa.me/${cleanPhone}?text=${text}`;
  }

  openWhatsApp(): void {
    window.open(this.whatsappUrl, '_blank', 'noopener,noreferrer');
  }

  onSubmit(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSending = true;
    setTimeout(() => {
      this.isSending = false;
      this.isSubmitted = true;
    }, 400);
  }

  close(): void {
    this.dialogRef.close(this.isSubmitted);
  }
}
