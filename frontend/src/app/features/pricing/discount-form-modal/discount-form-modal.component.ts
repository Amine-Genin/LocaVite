import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Discount, DiscountRequest, DiscountType } from '../services/pricing-admin.service';

export interface DiscountFormModalData {
  discount?: Discount;
}

@Component({
  selector: 'app-discount-form-modal',
  standalone: true,
  providers: [provideNativeDateAdapter()],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './discount-form-modal.component.html',
  styleUrl: '../season-form-modal/season-form-modal.component.scss',
})
export class DiscountFormModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<DiscountFormModalComponent>);
  readonly data = inject<DiscountFormModalData>(MAT_DIALOG_DATA);

  readonly isEdit = Boolean(this.data?.discount);

  readonly form = this.fb.group({
    type: [this.data?.discount?.type ?? ('PROMO_CODE' as DiscountType), [Validators.required]],
    libelle: [this.data?.discount?.libelle ?? '', [Validators.required, Validators.maxLength(150)]],
    pourcentage: [
      this.data?.discount?.pourcentage ?? 10,
      [Validators.required, Validators.min(0.01), Validators.max(100)],
    ],
    minDays: [this.data?.discount?.minDays ?? 8],
    code: [this.data?.discount?.code ?? ''],
    dateDebut: [this.parseDate(this.data?.discount?.dateDebut)],
    dateFin: [this.parseDate(this.data?.discount?.dateFin)],
    actif: [this.data?.discount?.actif ?? true],
  });

  get isPromoCode(): boolean {
    return this.form.get('type')?.value === 'PROMO_CODE';
  }

  get canSubmit(): boolean {
    if (this.form.invalid) return false;
    if (this.isPromoCode) {
      return Boolean((this.form.get('code')?.value ?? '').trim());
    }
    return Number(this.form.get('minDays')?.value) >= 1;
  }

  submit(): void {
    if (!this.canSubmit) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const isPromo = v.type === 'PROMO_CODE';
    const payload: DiscountRequest = {
      type: v.type as DiscountType,
      libelle: (v.libelle ?? '').trim(),
      pourcentage: Number(v.pourcentage),
      code: isPromo ? (v.code ?? '').trim().toUpperCase() : null,
      minDays: !isPromo ? Number(v.minDays) : null,
      dateDebut: v.dateDebut ? this.formatIsoDate(v.dateDebut as Date) : null,
      dateFin: v.dateFin ? this.formatIsoDate(v.dateFin as Date) : null,
      actif: Boolean(v.actif),
    };
    this.dialogRef.close(payload);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private parseDate(iso?: string | null): Date | null {
    if (!iso) return null;
    const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
    return new Date(y, (m || 1) - 1, d || 1);
  }

  private formatIsoDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
