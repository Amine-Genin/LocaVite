import { Injectable, inject, signal, computed } from '@angular/core';
import { DOCUMENT, registerLocaleData, DatePipe, DecimalPipe, CurrencyPipe } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import localeAr from '@angular/common/locales/ar';
import localeEn from '@angular/common/locales/en';
import { Direction, Directionality } from '@angular/cdk/bidi';
import { TranslateService } from '@ngx-translate/core';

// Always register supported locales for Angular DatePipe, DecimalPipe, and CurrencyPipe
registerLocaleData(localeFr, 'fr');
registerLocaleData(localeAr, 'ar');
registerLocaleData(localeEn, 'en');

export type SupportedLang = 'fr' | 'ar' | 'en';
export type { Direction };

export const LANG_KEY = 'locavite_lang';
export const DEFAULT_LANG: SupportedLang = 'fr';
export const SUPPORTED_LANGS: readonly SupportedLang[] = ['fr', 'ar', 'en'];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);
  private dirService = inject(Directionality, { optional: true });
  private document = inject(DOCUMENT);

  currentLang = signal<SupportedLang>(DEFAULT_LANG);
  direction = computed<Direction>(() => (this.currentLang() === 'ar' ? 'rtl' : 'ltr'));

  private datePipe = new DatePipe(DEFAULT_LANG);
  private decimalPipe = new DecimalPipe(DEFAULT_LANG);
  private currencyPipe = new CurrencyPipe(DEFAULT_LANG);

  init(): void {
    this.translate.addLangs([...SUPPORTED_LANGS]);
    this.translate.setDefaultLang(DEFAULT_LANG);

    let saved: SupportedLang = DEFAULT_LANG;
    try {
      const item = localStorage.getItem(LANG_KEY);
      if (item && (SUPPORTED_LANGS as readonly string[]).includes(item)) {
        saved = item as SupportedLang;
      }
    } catch {
      // LocalStorage might be disabled or unavailable
    }

    this.setLanguage(saved);
  }

  setLanguage(lang: SupportedLang): void {
    const validLang: SupportedLang = (SUPPORTED_LANGS as readonly string[]).includes(lang)
      ? lang
      : DEFAULT_LANG;

    this.currentLang.set(validLang);

    try {
      localStorage.setItem(LANG_KEY, validLang);
    } catch {
      // Ignore storage errors in restricted contexts
    }

    this.translate.use(validLang);

    const dir: Direction = validLang === 'ar' ? 'rtl' : 'ltr';

    if (this.document) {
      if (this.document.documentElement) {
        this.document.documentElement.setAttribute('dir', dir);
        this.document.documentElement.setAttribute('lang', validLang);
      }
      if (this.document.body) {
        this.document.body.setAttribute('dir', dir);
      }
    }

    if (this.dirService) {
      (this.dirService as { value: Direction }).value = dir;
      this.dirService.change.emit(dir);
    }
  }

  isRtl(): boolean {
    return this.direction() === 'rtl';
  }

  formatDate(
    value: Date | string | number | null | undefined,
    format = 'mediumDate',
    timezone?: string,
  ): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.datePipe.transform(value, format, timezone, this.currentLang());
  }

  formatNumber(value: number | string | null | undefined, digitsInfo?: string): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.decimalPipe.transform(value, digitsInfo, this.currentLang());
  }

  formatCurrency(
    value: number | string | null | undefined,
    currencyCode = 'MAD',
    display: 'code' | 'symbol' | 'symbol-narrow' | string = 'symbol',
    digitsInfo?: string,
  ): string | null {
    if (value === null || value === undefined || value === '') return null;
    return this.currencyPipe.transform(value, currencyCode, display, digitsInfo, this.currentLang());
  }
}
