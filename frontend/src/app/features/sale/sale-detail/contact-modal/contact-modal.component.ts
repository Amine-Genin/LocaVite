import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SaleListingContactService } from '../../../vehicles/services/sale-listing-contact.service';
import { SaleListing } from '../../models/sale-listing.model';

export interface ContactModalData {
  listing: SaleListing;
}

@Component({
  selector: 'app-contact-modal',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    TranslateModule,
  ],
  templateUrl: './contact-modal.component.html',
  styleUrl: './contact-modal.component.scss',
})
export class ContactModalComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly contactService = inject(SaleListingContactService);
  private readonly auth = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<ContactModalComponent>);

  readonly form = this.fb.group({
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['', Validators.required],
    message: ['', Validators.required],
  });

  sending = false;
  submitted = false;
  failed = false;

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: ContactModalData) {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.form.patchValue({
        nom: `${user.prenom} ${user.nom}`.trim(),
        email: user.email,
        telephone: user.telephone,
      });
    }
    this.form.controls.message.setValue(
      `Bonjour, je suis intéressé(e) par ${data.listing.marque} ${data.listing.modele} (${data.listing.annee}).`,
    );
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.sending = true;
    this.failed = false;
    this.contactService
      .sendContactRequest(this.data.listing.id, this.form.getRawValue())
      .pipe(finalize(() => (this.sending = false)))
      .subscribe({
        next: () => (this.submitted = true),
        error: () => (this.failed = true),
      });
  }

  close(): void {
    this.dialogRef.close(this.submitted);
  }
}
