import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validateur de mot de passe fort :
 * Au moins 8 caractères, au moins 1 majuscule et au moins 1 chiffre.
 */
export const strongPasswordValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;

  const hasUpperCase = /[A-Z]/.test(value);
  const hasDigit = /\d/.test(value);
  const hasMinLength = value.length >= 8;

  const valid = hasUpperCase && hasDigit && hasMinLength;
  return valid ? null : { strongPassword: true };
};

/**
 * Validateur pour numéros de téléphone marocains :
 * ex: 0612345678, 0712345678, 0522123456, +212612345678
 */
export const moroccanPhoneValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;

  const regex = /^(\+212|0)[5-7]\d{8}$/;
  return regex.test(value) ? null : { invalidPhone: true };
};

/**
 * Validateur vérifiant la correspondance entre deux champs de mot de passe.
 */
export function passwordMatchValidator(controlName: string, matchingControlName: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const control = group.get(controlName);
    const matchingControl = group.get(matchingControlName);

    if (!control || !matchingControl) return null;

    // Ne pas écraser d'autres erreurs déjà présentes sur le champ confirmPassword (ex: required)
    if (matchingControl.errors && !matchingControl.errors['passwordMismatch']) {
      return null;
    }

    if (control.value !== matchingControl.value) {
      matchingControl.setErrors({ ...matchingControl.errors, passwordMismatch: true });
      return { passwordMismatch: true };
    } else {
      if (matchingControl.errors?.['passwordMismatch']) {
        const errors = { ...matchingControl.errors };
        delete errors['passwordMismatch'];
        matchingControl.setErrors(Object.keys(errors).length > 0 ? errors : null);
      }
      return null;
    }
  };
}

