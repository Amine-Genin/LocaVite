import { Pipe, PipeTransform, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LanguageService } from './language.service';

@Pipe({
  name: 'localizedDate',
  standalone: true,
  pure: false,
})
export class LocalizedDatePipe implements PipeTransform {
  private langService = inject(LanguageService);
  private datePipe = new DatePipe('fr');

  transform(
    value: Date | string | number | null | undefined,
    format = 'mediumDate',
    timezone?: string,
  ): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.datePipe.transform(value, format, timezone, this.langService.currentLang());
  }
}
