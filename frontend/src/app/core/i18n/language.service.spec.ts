import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Directionality } from '@angular/cdk/bidi';
import { DOCUMENT } from '@angular/common';
import { LanguageService, LANG_KEY } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;
  let dirService: Directionality;
  let doc: Document;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [LanguageService, Directionality],
    });

    service = TestBed.inject(LanguageService);
    dirService = TestBed.inject(Directionality);
    doc = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    localStorage.clear();
    doc.documentElement.removeAttribute('dir');
    doc.documentElement.removeAttribute('lang');
    if (doc.body) {
      doc.body.removeAttribute('dir');
    }
  });

  it('should initialize with default language fr when localStorage is empty', () => {
    service.init();

    expect(service.currentLang()).toBe('fr');
    expect(service.direction()).toBe('ltr');
    expect(service.isRtl()).toBeFalse();
    expect(localStorage.getItem(LANG_KEY)).toBe('fr');
    expect(doc.documentElement.getAttribute('dir')).toBe('ltr');
    expect(doc.documentElement.getAttribute('lang')).toBe('fr');
  });

  it('should restore saved language from localStorage on init', () => {
    localStorage.setItem(LANG_KEY, 'ar');

    service.init();

    expect(service.currentLang()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(service.isRtl()).toBeTrue();
    expect(doc.documentElement.getAttribute('dir')).toBe('rtl');
    expect(doc.documentElement.getAttribute('lang')).toBe('ar');
    if (doc.body) {
      expect(doc.body.getAttribute('dir')).toBe('rtl');
    }
  });

  it('should switch language to English and set ltr', () => {
    service.setLanguage('en');

    expect(service.currentLang()).toBe('en');
    expect(service.direction()).toBe('ltr');
    expect(service.isRtl()).toBeFalse();
    expect(localStorage.getItem(LANG_KEY)).toBe('en');
    expect(doc.documentElement.getAttribute('dir')).toBe('ltr');
    expect(doc.documentElement.getAttribute('lang')).toBe('en');
  });

  it('should switch language to Arabic and set rtl automatically', () => {
    service.setLanguage('ar');

    expect(service.currentLang()).toBe('ar');
    expect(service.direction()).toBe('rtl');
    expect(service.isRtl()).toBeTrue();
    expect(localStorage.getItem(LANG_KEY)).toBe('ar');
    expect(doc.documentElement.getAttribute('dir')).toBe('rtl');
    expect(doc.documentElement.getAttribute('lang')).toBe('ar');
    if (doc.body) {
      expect(doc.body.getAttribute('dir')).toBe('rtl');
    }
    expect(dirService.value).toBe('rtl');
  });

  it('should notify CDK Directionality on change', () => {
    spyOn(dirService.change, 'emit');

    service.setLanguage('ar');
    expect(dirService.change.emit).toHaveBeenCalledWith('rtl');

    service.setLanguage('fr');
    expect(dirService.change.emit).toHaveBeenCalledWith('ltr');
  });

  it('should format dates according to the active language', () => {
    const testDate = new Date(2026, 0, 15); // Jan 15, 2026

    service.setLanguage('fr');
    const frFormatted = service.formatDate(testDate, 'mediumDate');
    expect(frFormatted).toBeTruthy();

    service.setLanguage('en');
    const enFormatted = service.formatDate(testDate, 'mediumDate');
    expect(enFormatted).toBeTruthy();

    service.setLanguage('ar');
    const arFormatted = service.formatDate(testDate, 'mediumDate');
    expect(arFormatted).toBeTruthy();
  });

  it('should format numbers according to the active language', () => {
    const num = 123456.78;

    service.setLanguage('fr');
    const frNum = service.formatNumber(num);
    expect(frNum).toBeTruthy();

    service.setLanguage('en');
    const enNum = service.formatNumber(num);
    expect(enNum).toBeTruthy();
  });

  it('should format currency according to the active language', () => {
    const price = 450;

    service.setLanguage('fr');
    const frCur = service.formatCurrency(price, 'MAD');
    expect(frCur).toBeTruthy();

    service.setLanguage('en');
    const enCur = service.formatCurrency(price, 'MAD');
    expect(enCur).toBeTruthy();
  });
});
