import { Component, Inject, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SaleListing } from '../../models/sale-listing.model';
import { SaleVisitService, SaleVisitType } from '../../services/sale-visit.service';

export interface VisitModalData {
  listing: SaleListing;
}

@Component({
  selector: 'app-visit-modal',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslateModule,
  ],
  templateUrl: './visit-modal.component.html',
  styleUrl: './visit-modal.component.scss',
})
export class VisitModalComponent {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly visitService = inject(SaleVisitService);
  private readonly auth = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<VisitModalComponent>);

  readonly visitTypes: SaleVisitType[] = ['VISITE_SIMPLE', 'ESSAI_ROUTIER', 'INSPECTION_MECANIQUE'];
  readonly form = this.fb.group({
    dateSouhaitee: ['', Validators.required],
    creneauHoraire: ['', Validators.required],
    lieu: ['', Validators.required],
    typeVisite: ['VISITE_SIMPLE' as SaleVisitType, Validators.required],
  });

  sending = false;
  submitted = false;
  failed = false;

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: VisitModalData) {}

  get authenticated(): boolean {
    return this.auth.isAuthenticated();
  }

  get minDate(): string {
    const today = new Date();
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(
      today.getDate(),
    ).padStart(2, '0')}`;
  }

  submit(): void {
    if (!this.authenticated) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.sending = true;
    this.failed = false;
    this.visitService
      .request(this.data.listing.id, this.form.getRawValue())
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
