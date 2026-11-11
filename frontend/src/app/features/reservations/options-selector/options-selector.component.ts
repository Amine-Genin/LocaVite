import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';

export interface OptionsFormControls {
  gps: FormControl<boolean>;
  babySeat: FormControl<boolean>;
  extraDriver: FormControl<boolean>;
}

@Component({
  selector: 'app-options-selector',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCheckboxModule],
  template: `
    <div [formGroup]="form" class="options" data-cy="options-selector">
      <mat-checkbox formControlName="gps" data-cy="option-gps">GPS</mat-checkbox>
      <mat-checkbox formControlName="babySeat" data-cy="option-baby-seat">Siège bébé</mat-checkbox>
      <mat-checkbox formControlName="extraDriver" data-cy="option-extra-driver">
        Conducteur additionnel
      </mat-checkbox>
    </div>
  `,
  styles: [
    `
      .options {
        display: flex;
        flex-wrap: wrap;
        gap: 1.25rem;
      }
    `,
  ],
})
export class OptionsSelectorComponent {
  @Input({ required: true }) form!: FormGroup<OptionsFormControls>;
}
