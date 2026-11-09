import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { BidiModule } from '@angular/cdk/bidi';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageService } from './core/i18n/language.service';
import { NetworkService } from './core/services/network.service';
import { PwaInstallPromptComponent } from './shared/components/pwa-install-prompt/pwa-install-prompt.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, BidiModule, TranslateModule, PwaInstallPromptComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  host: {
    '[attr.dir]': 'langService.direction()',
    '[attr.lang]': 'langService.currentLang()',
  },
})
export class AppComponent implements OnInit {
  title = 'locavite';
  langService = inject(LanguageService);
  networkService = inject(NetworkService);

  ngOnInit(): void {
    this.langService.init();
  }
}
