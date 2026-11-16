import { Component, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { BidiModule } from '@angular/cdk/bidi';
import { TranslateModule } from '@ngx-translate/core';
import { filter } from 'rxjs';
import { LanguageService } from './core/i18n/language.service';
import { NetworkService } from './core/services/network.service';
import { PwaInstallPromptComponent } from './shared/components/pwa-install-prompt/pwa-install-prompt.component';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    BidiModule,
    TranslateModule,
    PwaInstallPromptComponent,
    NavbarComponent,
  ],
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
  private router = inject(Router);

  showNavbar = signal(!this.isAuthRoute(this.router.url));

  ngOnInit(): void {
    this.langService.init();
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.showNavbar.set(!this.isAuthRoute(event.urlAfterRedirects));
      });
  }

  private isAuthRoute(url: string): boolean {
    const clean = url.split('?')[0];
    return clean === '/login' || clean === '/register';
  }
}
