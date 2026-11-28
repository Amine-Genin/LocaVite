import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Season, SeasonRequest } from '../services/pricing-admin.service';

export interface SeasonFormModalData {
  season?: Season;
  existingSeasons: Season[];
}

@Component({
  selector: 'app-season-form-modal',
  standalone: true,
  providers: [provideNativeDateAdapter()],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './season-form-modal.component.html',
  styleUrl: './season-form-modal.component.scss',
})
export class SeasonFormModalComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<SeasonFormModalComponent>);
  readonly data = inject<SeasonFormModalData>(MAT_DIALOG_DATA);

  readonly isEdit = Boolean(this.data?.season);

  readonly form = this.fb.group(
    {
      nom: [this.data?.season?.nom ?? '', [Validators.required, Validators.maxLength(100)]],
      dateDebut: [this.parseDate(this.data?.season?.dateDebut), [Validators.required]],
      dateFin: [this.parseDate(this.data?.season?.dateFin), [Validators.required]],
      coefficient: [
        this.data?.season?.coefficient ?? 1.2,
        [Validators.required, Validators.min(0.01), Validators.max(10)],
      ],
      priorite: [
        this.data?.season?.priorite ?? 1,
        [Validators.required, Validators.min(1), Validators.max(100)],
      ],
      agenceId: [this.data?.season?.agenceId ?? null],
      isRecurring: [this.data?.season?.isRecurring ?? false],
    },
    { validators: [this.dateRangeValidator(), this.overlapValidator()] },
  );

  get overlapErrorMessage(): string | null {
    const err = this.form.errors?.['seasonOverlap'] as { message?: string } | undefined;
    return err?.message ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const payload: SeasonRequest = {
      nom: (v.nom ?? '').trim(),
      dateDebut: this.formatIsoDate(v.dateDebut as Date),
      dateFin: this.formatIsoDate(v.dateFin as Date),
      coefficient: Number(v.coefficient),
      priorite: Number(v.priorite),
      agenceId: v.agenceId !== null && v.agenceId !== undefined && String(v.agenceId) !== ''
        ? Number(v.agenceId)
        : null,
      isRecurring: Boolean(v.isRecurring),
    };

    this.dialogRef.close(payload);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private dateRangeValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const start = group.get('dateDebut')?.value as Date | null;
      const end = group.get('dateFin')?.value as Date | null;
      if (!start || !end) return null;
      return start.getTime() < end.getTime() ? null : { invalidDateRange: true };
    };
  }

  private overlapValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const start = group.get('dateDebut')?.value as Date | null;
      const end = group.get('dateFin')?.value as Date | null;
      const priorite = Number(group.get('priorite')?.value);
      const rawAgenceId = group.get('agenceId')?.value;
      const agenceId =
        rawAgenceId !== null && rawAgenceId !== undefined && String(rawAgenceId) !== ''
          ? Number(rawAgenceId)
          : null;

      if (!start || !end || start.getTime() >= end.getTime() || !Number.isFinite(priorite)) {
        return null;
      }

      const startIso = this.formatIsoDate(start);
      const endIso = this.formatIsoDate(end);
      const currentId = this.data?.season?.id;

      const conflict = (this.data?.existingSeasons ?? []).find((other) => {
        if (currentId !== undefined && other.id === currentId) return false;
        const otherAgence = other.agenceId ?? null;
        if (otherAgence !== agenceId || (other.vehicleId ?? null) !== null) return false;
        const overlaps = startIso <= other.dateFin && other.dateDebut <= endIso;
        return overlaps && other.priorite === priorite;
      });

      if (!conflict) return null;
      return {
        seasonOverlap: {
          message: `Chevauchement détecté avec "${conflict.nom}" (${conflict.dateDebut} → ${conflict.dateFin}) ayant la même priorité (${priorite}). Choisissez une priorité différente.`,
        },
      };
    };
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
