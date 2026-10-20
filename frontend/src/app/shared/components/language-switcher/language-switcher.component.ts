import { Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService, SupportedLang } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [MatButtonToggleModule, TranslateModule],
  template: `
    <div class="lang-switcher">
      <mat-button-toggle-group
        [value]="langService.currentLang()"
        (change)="onLangChange($event.value)"
        aria-label="Sélection de la langue"
      >
        <mat-button-toggle value="fr">FR</mat-button-toggle>
        <mat-button-toggle value="ar">العربية</mat-button-toggle>
        <mat-button-toggle value="en">EN</mat-button-toggle>
      </mat-button-toggle-group>
    </div>
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
  `],
})
export class LanguageSwitcherComponent {
  langService = inject(LanguageService);

  onLangChange(lang: SupportedLang) {
    this.langService.setLanguage(lang);
  }
}

