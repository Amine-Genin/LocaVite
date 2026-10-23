import { Pipe, PipeTransform, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { LanguageService } from './language.service';

@Pipe({
  name: 'localizedCurrency',
  standalone: true,
  pure: false,
})
export class LocalizedCurrencyPipe implements PipeTransform {
  private langService = inject(LanguageService);
  private currencyPipe = new CurrencyPipe('fr');

  transform(
    value: number | string | null | undefined,
    currencyCode = 'MAD',
    display: 'code' | 'symbol' | 'symbol-narrow' | string = 'symbol',
    digitsInfo?: string,
  ): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.currencyPipe.transform(value, currencyCode, display, digitsInfo, this.langService.currentLang());
  }
}
