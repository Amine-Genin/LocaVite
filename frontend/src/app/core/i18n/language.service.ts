import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type SupportedLang = 'fr' | 'ar' | 'en';

const LANG_KEY = 'locavite_lang';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private translate = inject(TranslateService);

  currentLang = signal<SupportedLang>('fr');

  init() {
    this.translate.addLangs(['fr', 'ar', 'en']);
    this.translate.setDefaultLang('fr');

    const saved = (localStorage.getItem(LANG_KEY) as SupportedLang) || 'fr';
    this.setLanguage(saved);
  }

  setLanguage(lang: SupportedLang) {
    this.currentLang.set(lang);
    localStorage.setItem(LANG_KEY, lang);
    this.translate.use(lang);

    const dir = lang === 'ar' ? 'rtl' : 'ltr';
    document.documentElement.setAttribute('dir', dir);
    document.documentElement.setAttribute('lang', lang);
  }
}

