import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService, SupportedLang } from '../../../core/i18n/language.service';

export type SwitcherVariant = 'toggle' | 'buttons';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [CommonModule, MatButtonToggleModule, TranslateModule],
  template: `
    @if (variant === 'toggle') {
      <div class="lang-switcher">
        <mat-button-toggle-group
          [value]="langService.currentLang()"
          (change)="onLangChange($event.value)"
          [attr.aria-label]="'COMMON.LANGUAGE' | translate"
        >
          <mat-button-toggle value="fr" aria-label="Français">FR</mat-button-toggle>
          <mat-button-toggle value="en" aria-label="English">EN</mat-button-toggle>
          <mat-button-toggle value="ar" aria-label="العربية">العربية</mat-button-toggle>
        </mat-button-toggle-group>
      </div>
    } @else {
      <div class="lang-bar" role="group" [attr.aria-label]="'COMMON.LANGUAGE' | translate">
        <button
          type="button"
          class="lang-btn"
          [class.active]="langService.currentLang() === 'fr'"
          (click)="onLangChange('fr')"
          aria-label="Français"
        >
          FR
        </button>
        <span class="dot">•</span>
        <button
          type="button"
          class="lang-btn"
          [class.active]="langService.currentLang() === 'en'"
          (click)="onLangChange('en')"
          aria-label="English"
        >
          EN
        </button>
        <span class="dot">•</span>
        <button
          type="button"
          class="lang-btn"
          [class.active]="langService.currentLang() === 'ar'"
          (click)="onLangChange('ar')"
          aria-label="العربية"
        >
          العربية
        </button>
      </div>
    }
  `,
  styles: [`
    .lang-switcher {
      display: flex;
      justify-content: flex-end;
      margin-bottom: var(--sp-2);
    }
    mat-button-toggle-group {
      border-radius: var(--radius);
      overflow: hidden;
      border-color: var(--c-border);
      height: 36px;
    }
    mat-button-toggle {
      font-size: 13px;
      font-weight: 600;
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
  `],
})
export class LanguageSwitcherComponent {
  @Input() variant: SwitcherVariant = 'toggle';

  langService = inject(LanguageService);

  onLangChange(lang: SupportedLang): void {
    this.langService.setLanguage(lang);
  }
}
