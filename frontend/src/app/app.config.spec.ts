import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

import { appConfig, HttpLoaderFactory } from './app.config';
import { environment } from '../environments/environment';
import { environment as prodEnvironment } from '../environments/environment.prod';

describe('appConfig & environments', () => {
  it('should expose application providers', () => {
    expect(appConfig.providers).toBeDefined();
    expect(appConfig.providers.length).toBeGreaterThan(0);
  });

  it('should create a translate loader with i18n assets path', () => {
    const loader = HttpLoaderFactory({} as HttpClient);
    const loaderConfig = loader as unknown as { prefix: string; suffix: string };

    expect(loader).toBeInstanceOf(TranslateHttpLoader);
    expect(loaderConfig.prefix).toBe('/assets/i18n/');
    expect(loaderConfig.suffix).toBe('.json');
  });

  it('should define development and production environment configurations', () => {
    expect(environment.production).toBeFalse();
    expect(environment.apiUrl).toBe('/api');
    expect(environment.defaultLanguage).toBe('fr');
    expect(environment.supportedLanguages).toEqual(['fr', 'ar', 'en']);

    expect(prodEnvironment.production).toBeTrue();
    expect(prodEnvironment.apiUrl).toBe('/api');
    expect(prodEnvironment.defaultLanguage).toBe('fr');
    expect(prodEnvironment.supportedLanguages).toEqual(['fr', 'ar', 'en']);
  });
});
