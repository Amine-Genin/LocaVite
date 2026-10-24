import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

import { appConfig, HttpLoaderFactory } from './app.config';

describe('appConfig', () => {
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
});
