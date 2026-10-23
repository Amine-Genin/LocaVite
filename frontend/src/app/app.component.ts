import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BidiModule } from '@angular/cdk/bidi';
import { LanguageService } from './core/i18n/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, BidiModule],
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

  ngOnInit(): void {
    this.langService.init();
  }
}
