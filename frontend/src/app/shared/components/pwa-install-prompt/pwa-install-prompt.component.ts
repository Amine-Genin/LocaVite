import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { PwaInstallService } from '../../../core/services/pwa-install.service';

@Component({
  selector: 'app-pwa-install-prompt',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    @if (pwaService.canInstall()) {
      <aside
        class="pwa-install-card"
        role="region"
        aria-label="Installation de l'application"
      >
        <div class="pwa-install-body">
          <img
            src="icons/icon-96x96.png"
            alt="LocaVite Logo"
            class="pwa-app-icon"
            width="48"
            height="48"
          />
          <div class="pwa-text-content">
            <h3 class="pwa-title">{{ 'PWA.INSTALL_TITLE' | translate }}</h3>
            <p class="pwa-description">{{ 'PWA.INSTALL_DESC' | translate }}</p>
          </div>
        </div>

        <div class="pwa-actions">
          <button
            type="button"
            class="btn-dismiss"
            (click)="dismiss()"
            [attr.aria-label]="'PWA.LATER' | translate"
          >
            {{ 'PWA.LATER' | translate }}
          </button>
          <button
            type="button"
            class="btn-install"
            (click)="install()"
            [attr.aria-label]="'PWA.INSTALL_BTN' | translate"
          >
            {{ 'PWA.INSTALL_BTN' | translate }}
          </button>
        </div>
      </aside>
    }
  `,
  styles: [
    `
      .pwa-install-card {
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 1000;
        background: #ffffff;
        border-radius: 14px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15), 0 2px 6px rgba(0, 0, 0, 0.08);
        padding: 16px;
        max-width: 420px;
        display: flex;
        flex-direction: column;
        gap: 14px;
        border: 1px solid rgba(63, 81, 181, 0.12);
        animation: pwaSlideUp 0.3s ease-out;

        @media (max-width: 600px) {
          left: 12px;
          right: 12px;
          bottom: 12px;
          max-width: none;
        }
      }

      .pwa-install-body {
        display: flex;
        align-items: center;
        gap: 14px;
      }

      .pwa-app-icon {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
        flex-shrink: 0;
      }

      .pwa-text-content {
        flex: 1;
        min-width: 0;
      }

      .pwa-title {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #1a237e;
      }

      .pwa-description {
        margin: 4px 0 0;
        font-size: 12px;
        color: #5c6b73;
        line-height: 1.4;
      }

      .pwa-actions {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        gap: 10px;
      }

      .btn-dismiss {
        background: transparent;
        border: none;
        color: #757575;
        font-size: 13px;
        font-weight: 600;
        padding: 8px 14px;
        border-radius: 8px;
        cursor: pointer;
        transition: background-color 0.2s ease;

        &:hover {
          background-color: #f5f5f5;
          color: #333333;
        }
      }

      .btn-install {
        background: #3f51b5;
        color: #ffffff;
        border: none;
        font-size: 13px;
        font-weight: 600;
        padding: 8px 18px;
        border-radius: 8px;
        cursor: pointer;
        box-shadow: 0 2px 8px rgba(63, 81, 181, 0.3);
        transition: background-color 0.2s ease, transform 0.1s ease;

        &:hover {
          background: #303f9f;
        }

        &:active {
          transform: scale(0.98);
        }
      }

      @keyframes pwaSlideUp {
        from {
          opacity: 0;
          transform: translateY(20px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
})
export class PwaInstallPromptComponent {
  readonly pwaService = inject(PwaInstallService);

  install(): void {
    void this.pwaService.promptInstall();
  }

  dismiss(): void {
    this.pwaService.dismiss();
  }
}
