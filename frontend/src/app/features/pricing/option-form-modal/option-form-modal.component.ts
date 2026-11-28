import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ReservationOption } from '../../reservations/models/reservation.model';
import { OptionPrice, OptionPriceRequest } from '../services/pricing-admin.service';

export interface OptionFormModalData {
  option?: OptionPrice;
  existingTypes: ReservationOption[];
}

@Component({
  selector: 'app-option-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './option-form-modal.component.html',
  styleUrl: '../season-form-modal/season-form-modal.component.scss',
})
export class OptionFormModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<OptionFormModalComponent>);
  readonly data = inject<OptionFormModalData>(MAT_DIALOG_DATA);

  readonly isEdit = Boolean(this.data?.option);

  readonly optionTypes: { value: ReservationOption; label: string }[] = [
    { value: 'GPS', label: 'GPS' },
    { value: 'BABY_SEAT', label: 'Siège bébé (BABY_SEAT)' },
    { value: 'EXTRA_DRIVER', label: 'Conducteur additionnel (EXTRA_DRIVER)' },
  ];

  readonly form = this.fb.group({
    type: [this.data?.option?.type ?? ('GPS' as ReservationOption), [Validators.required]],
    libelle: [this.data?.option?.libelle ?? '', [Validators.required, Validators.maxLength(120)]],
    prixJour: [this.data?.option?.prixJour ?? 50, [Validators.required, Validators.min(0)]],
    actif: [this.data?.option?.actif ?? true],
  });

  isTypeDisabled(type: ReservationOption): boolean {
    if (this.data?.option?.type === type) return false;
    return (this.data?.existingTypes ?? []).includes(type);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const payload: OptionPriceRequest = {
      type: v.type as ReservationOption,
      libelle: (v.libelle ?? '').trim(),
      prixJour: Number(v.prixJour),
      actif: Boolean(v.actif),
    };
    this.dialogRef.close(payload);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
