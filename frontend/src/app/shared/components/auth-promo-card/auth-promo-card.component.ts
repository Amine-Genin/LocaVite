import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-auth-promo-card',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <aside class="promo-card">
      <div class="promo-image-wrapper">
        <img
          src="/assets/images/auth-banner.jpg"
          [alt]="'AUTH.PROMO.IMAGE_ALT' | translate"
          class="promo-image"
          loading="lazy"
        />
      </div>

      <div class="promo-content">
        <h2 class="promo-title">{{ 'AUTH.PROMO.TITLE' | translate }}</h2>
        <p class="promo-desc">{{ 'AUTH.PROMO.DESC' | translate }}</p>
        <div class="promo-features">
          <span>{{ 'AUTH.PROMO.FEATURES' | translate }}</span>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .promo-card {
      background: var(--c-navy);
      border-radius: 18px;
      padding: 24px;
      color: #ffffff;
      box-shadow: 0 10px 30px rgba(20, 46, 69, 0.15);
      display: flex;
      flex-direction: column;
    }

    .promo-image-wrapper {
      width: 100%;
      border-radius: 12px;
      overflow: hidden;
      aspect-ratio: 16 / 10;
      background: #0d2030;
    }

    .promo-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .promo-content {
      padding: 28px 12px 12px 12px;
    }

    .promo-title {
      font-size: 26px;
      font-weight: 700;
      line-height: 1.25;
      margin: 0 0 14px 0;
      color: #ffffff;
    }

    .promo-desc {
      font-size: 15px;
      line-height: 1.6;
      color: rgba(255, 255, 255, 0.85);
      margin: 0 0 32px 0;
    }

    .promo-features {
      font-size: 13px;
      color: rgba(255, 255, 255, 0.65);
      font-weight: 500;
      letter-spacing: 0.2px;
    }
  `],
})
export class AuthPromoCardComponent {}
