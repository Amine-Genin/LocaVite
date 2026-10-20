import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-auth-footer',
  standalone: true,
  imports: [TranslateModule],
  template: `
    <footer class="site-footer">
      <div class="footer-inner">
        <p class="slogan">{{ 'FOOTER.SLOGAN' | translate }}</p>
        <p class="cities">{{ 'FOOTER.CITIES' | translate }}</p>
        <p class="contact">{{ 'FOOTER.CONTACT' | translate }}</p>
        <p class="copyright">{{ 'FOOTER.COPYRIGHT' | translate }}</p>
      </div>
    </footer>
  `,
  styles: [`
    .site-footer {
      background: #10263a;
      color: #ffffff;
      padding: 56px 24px 40px 24px;
      margin-top: 64px;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
    }

    .footer-inner {
      max-width: 1360px;
      margin: 0 auto;
    }

    .slogan {
      font-size: 19px;
      font-weight: 700;
      letter-spacing: 0.3px;
      margin: 0 0 16px 0;
      color: #ffffff;
    }

    .cities {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.72);
      margin: 0 0 16px 0;
      letter-spacing: 0.2px;
    }

    .contact {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.72);
      margin: 0 0 32px 0;
    }

    .copyright {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.42);
      margin: 0;
      letter-spacing: 0.2px;
    }
  `],
})
export class AuthFooterComponent {}

