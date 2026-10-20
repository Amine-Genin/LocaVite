import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService, SupportedLang } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-auth-header',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  template: `
    <header class="site-header">
      <div class="header-inner">
        <!-- Logo -->
        <a routerLink="/login" class="brand">
          <svg class="brand-icon" width="26" height="26" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M14 2L2.5 25H7.5L14 11.5L20.5 25H25.5L14 2Z" fill="var(--c-primary)" />
            <path d="M14 16L10.5 23H17.5L14 16Z" fill="var(--c-primary)" />
          </svg>
          <span class="brand-text">LOCAVITE</span>
        </a>

        <!-- Main Navigation Links -->
        <nav class="nav-links">
          <a href="javascript:void(0)" class="nav-link">{{ 'NAV.RENT_CAR' | translate }}</a>
          <a href="javascript:void(0)" class="nav-link">{{ 'NAV.BUY_CAR' | translate }}</a>
          <a href="javascript:void(0)" class="nav-link">{{ 'NAV.AGENCIES' | translate }}</a>
          <a href="javascript:void(0)" class="nav-link">{{ 'NAV.HOW_IT_WORKS' | translate }}</a>
        </nav>

        <!-- Right: Language Switcher & Mon Espace -->
        <div class="header-right">
          <div class="lang-bar">
            <button
              type="button"
              class="lang-btn"
              [class.active]="langService.currentLang() === 'fr'"
              (click)="setLang('fr')"
            >
              FR
            </button>
            <span class="dot">•</span>
            <button
              type="button"
              class="lang-btn"
              [class.active]="langService.currentLang() === 'en'"
              (click)="setLang('en')"
            >
              EN
            </button>
            <span class="dot">•</span>
            <button
              type="button"
              class="lang-btn"
              [class.active]="langService.currentLang() === 'ar'"
              (click)="setLang('ar')"
            >
              العربية
            </button>
          </div>
          <a routerLink="/login" class="user-space-link">{{ 'NAV.MY_SPACE' | translate }}</a>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .site-header {
      background: #ffffff;
      border-bottom: 1px solid #edf1f6;
      position: sticky;
      top: 0;
      z-index: 50;
    }

    .header-inner {
      max-width: 1360px;
      margin: 0 auto;
      padding: 16px 32px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
    }

    .brand {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      flex-shrink: 0;
    }

    .brand-icon {
      display: block;
    }

    .brand-text {
      font-size: 20px;
      font-weight: 800;
      letter-spacing: 0.8px;
      color: var(--c-navy);
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 28px;
    }

    .nav-link {
      font-size: 14px;
      font-weight: 500;
      color: var(--c-navy);
      text-decoration: none;
      transition: color 0.15s ease;

      &:hover {
        color: var(--c-primary);
      }
    }

    .header-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 3px;
    }

    .lang-bar {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      color: #607282;
    }

    .lang-btn {
      background: none;
      border: none;
      padding: 0;
      font-size: 12px;
      font-weight: 600;
      color: #7b8e9f;
      cursor: pointer;
      font-family: inherit;
      transition: color 0.15s ease;

      &:hover {
        color: var(--c-navy);
      }

      &.active {
        color: var(--c-navy);
        font-weight: 700;
      }
    }

    .dot {
      color: #a8b8c6;
      font-size: 11px;
    }

    .user-space-link {
      font-size: 12px;
      font-weight: 500;
      color: #607282;
      text-decoration: none;

      &:hover {
        color: var(--c-navy);
        text-decoration: underline;
      }
    }

    @media (max-width: 900px) {
      .nav-links {
        display: none;
      }
      .header-inner {
        padding: 14px 20px;
      }
    }
  `],
})
export class AuthHeaderComponent {
  langService = inject(LanguageService);

  setLang(lang: SupportedLang) {
    this.langService.setLanguage(lang);
  }
}

