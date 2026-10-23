import { Pipe, PipeTransform, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { LanguageService } from './language.service';

@Pipe({
  name: 'localizedNumber',
  standalone: true,
  pure: false,
})
export class LocalizedNumberPipe implements PipeTransform {
  private langService = inject(LanguageService);
  private decimalPipe = new DecimalPipe('fr');

  transform(value: number | string | null | undefined, digitsInfo?: string): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.decimalPipe.transform(value, digitsInfo, this.langService.currentLang());
  }
}
