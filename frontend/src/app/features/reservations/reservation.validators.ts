import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function minTodayValidator(referenceDateFn: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as Date | null;
    if (!value || !(value instanceof Date) || Number.isNaN(value.getTime())) {
      return null;
    }
    const min = startOfDay(referenceDateFn());
    const selected = startOfDay(value);
    return selected < min ? { minDate: true } : null;
  };
}

export const dateRangeValidator: ValidatorFn = (
  group: AbstractControl,
): ValidationErrors | null => {
  const start = group.get('dateDebut')?.value as Date | null;
  const end = group.get('dateFin')?.value as Date | null;

  if (!start || !end || !(start instanceof Date) || !(end instanceof Date)) {
    return null;
  }
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return null;
  }

  return startOfDay(end) <= startOfDay(start) ? { invalidDateRange: true } : null;
};
