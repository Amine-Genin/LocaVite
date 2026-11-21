import { Component, Inject, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Subscription, finalize } from 'rxjs';

import { Vehicle } from '../../models/vehicle.model';
import { AuthService } from '../../../../core/auth/auth.service';
import { SaleListingContactService } from '../../services/sale-listing-contact.service';
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
export class VehicleContactDialogComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<VehicleContactDialogComponent>);
  private readonly translate = inject(TranslateService, { optional: true });
  private readonly authService = inject(AuthService);
  private readonly contactService = inject(SaleListingContactService);

  contactForm: FormGroup;
  isSending = false;
  isSubmitted = false;
  isLoadingListing = true;
  listingLookupFailed = false;
  submitFailed = false;
  saleListingId: number | null = null;

  readonly vehicle: Vehicle;
  readonly defaultPhone = '+212600000000';
  private listingSubscription?: Subscription;

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

    const user = this.authService.getCurrentUser();
    if (user) {
      this.contactForm.patchValue({
        nom: `${user.prenom} ${user.nom}`.trim(),
        email: user.email,
        telephone: user.telephone,
      });
    }

    this.listingSubscription = this.contactService
      .getPublishedListingIdByVehicle(this.vehicle.id)
      .subscribe({
        next: (id) => {
          this.saleListingId = id;
          this.isLoadingListing = false;
          this.listingLookupFailed = id === null;
        },
        error: () => {
          this.isLoadingListing = false;
          this.listingLookupFailed = true;
        },
      });
  }

  ngOnDestroy(): void {
    this.listingSubscription?.unsubscribe();
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

    if (this.saleListingId === null || this.isLoadingListing) {
      return;
    }

    this.isSending = true;
    this.submitFailed = false;
    this.contactService
      .sendContactRequest(this.saleListingId, this.contactForm.getRawValue())
      .pipe(finalize(() => (this.isSending = false)))
      .subscribe({
        next: () => {
          this.isSubmitted = true;
        },
        error: () => {
          this.submitFailed = true;
        },
      });
  }

  close(): void {
    this.dialogRef.close(this.isSubmitted);
  }
}
