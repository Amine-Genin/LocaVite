import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DatesFormControls {
  dateDebut: FormControl<Date | null>;
  dateFin: FormControl<Date | null>;
}

@Component({
  selector: 'app-date-range-picker',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <div [formGroup]="form" class="date-range-picker" data-cy="date-range-picker">
      <div class="fields two-columns">
        <mat-form-field appearance="outline">
          <mat-label>Date de début</mat-label>
          <input
            matInput
            [matDatepicker]="startPicker"
            [min]="minDate"
            formControlName="dateDebut"
            data-cy="date-debut-input"
          />
          <mat-datepicker-toggle matIconSuffix [for]="startPicker"></mat-datepicker-toggle>
          <mat-datepicker #startPicker></mat-datepicker>
          @if (form.controls.dateDebut.hasError('required')) {
            <mat-error>La date de début est obligatoire.</mat-error>
          } @else if (
            form.controls.dateDebut.hasError('minDate') ||
            form.controls.dateDebut.hasError('matDatepickerMin')
          ) {
            <mat-error>La date de début ne peut pas être dans le passé.</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Date de fin</mat-label>
          <input
            matInput
            [matDatepicker]="endPicker"
            [min]="minDate"
            formControlName="dateFin"
            data-cy="date-fin-input"
          />
          <mat-datepicker-toggle matIconSuffix [for]="endPicker"></mat-datepicker-toggle>
          <mat-datepicker #endPicker></mat-datepicker>
          @if (form.controls.dateFin.hasError('required')) {
            <mat-error>La date de fin est obligatoire.</mat-error>
          }
        </mat-form-field>
      </div>

      @if (form.hasError('invalidDateRange')) {
        <p class="field-error" data-cy="date-range-error">
          La date de fin doit être postérieure à la date de début.
        </p>
      }
      @if (checking) {
        <p class="status" data-cy="availability-checking">Vérification de la disponibilité…</p>
      }
      @if (available === true) {
        <p class="status success" data-cy="availability-success">
          Véhicule disponible pour cette période.
        </p>
      }
      @if (available === false) {
        <p class="status error" data-cy="availability-error">
          Véhicule indisponible pour cette période.
        </p>
      }
    </div>
  `,
  styles: [
    `
      .date-range-picker {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
      .fields.two-columns {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
      }
      .status {
        font-weight: 600;
        margin: 0;
      }
      .success {
        color: #166534;
      }
      .error,
      .field-error {
        color: #b91c1c;
        margin: 0;
      }
      @media (max-width: 640px) {
        .fields.two-columns {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class DateRangePickerComponent {
  @Input({ required: true }) form!: FormGroup<DatesFormControls>;
  @Input() minDate: Date = new Date();
  @Input() checking = false;
  @Input() available: boolean | null = null;
}
