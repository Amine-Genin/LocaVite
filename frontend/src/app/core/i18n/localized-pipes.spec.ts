import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { LocalizedDatePipe } from './localized-date.pipe';
import { LocalizedNumberPipe } from './localized-number.pipe';
import { LocalizedCurrencyPipe } from './localized-currency.pipe';
import { LanguageService } from './language.service';

describe('Localized Pipes', () => {
  let langService: LanguageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [LanguageService, LocalizedDatePipe, LocalizedNumberPipe, LocalizedCurrencyPipe],
    });

    langService = TestBed.inject(LanguageService);
  });

  describe('LocalizedDatePipe', () => {
    it('should format date for current language', () => {
      const pipe = TestBed.inject(LocalizedDatePipe);
      const date = new Date(2026, 5, 20);

      langService.setLanguage('fr');
      const frDate = pipe.transform(date, 'mediumDate');
      expect(frDate).toBeTruthy();

      langService.setLanguage('en');
      const enDate = pipe.transform(date, 'mediumDate');
      expect(enDate).toBeTruthy();

      expect(pipe.transform(null)).toBeNull();
    });
  });

  describe('LocalizedNumberPipe', () => {
    it('should format number for current language', () => {
      const pipe = TestBed.inject(LocalizedNumberPipe);

      langService.setLanguage('fr');
      const frNum = pipe.transform(1234.56, '1.2-2');
      expect(frNum).toBeTruthy();

      langService.setLanguage('en');
      const enNum = pipe.transform(1234.56, '1.2-2');
      expect(enNum).toBeTruthy();

      expect(pipe.transform(null)).toBeNull();
    });
  });

  describe('LocalizedCurrencyPipe', () => {
    it('should format currency for current language', () => {
      const pipe = TestBed.inject(LocalizedCurrencyPipe);

      langService.setLanguage('fr');
      const frCurr = pipe.transform(500, 'MAD');
      expect(frCurr).toBeTruthy();

      langService.setLanguage('en');
      const enCurr = pipe.transform(500, 'MAD');
      expect(enCurr).toBeTruthy();

      expect(pipe.transform(null)).toBeNull();
    });
  });
});
