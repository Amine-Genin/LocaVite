import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { NetworkService } from '../../core/services/network.service';

@Component({
  selector: 'app-offline',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
  template: `
    <div class="offline-container">
      <div class="offline-card">
        <div class="offline-icon-wrapper" [class.is-online]="networkService.isOnline()">
          @if (networkService.isOnline()) {
            <svg class="offline-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12.55a11 11 0 0 1 14.08 0" />
              <path d="M1.42 9a16 16 0 0 1 21.16 0" />
              <path d="M8.53 16.11a6 6 0 0 1 6.95 0" />
              <line x1="12" y1="20" x2="12.01" y2="20" />
            </svg>
          } @else {
            <svg class="offline-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="1" y1="1" x2="23" y2="23" />
              <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55" />
              <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39" />
              <path d="M10.71 5.05A16 16 0 0 1 22.58 9" />
              <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88" />
              <path d="M8.53 16.11a6 6 0 0 1 6.95 0" />
              <line x1="12" y1="20" x2="12.01" y2="20" />
            </svg>
          }
        </div>

        <h1 class="offline-title">
          {{ (networkService.isOnline() ? 'OFFLINE.BACK_ONLINE_TITLE' : 'OFFLINE.TITLE') | translate }}
        </h1>

        <p class="offline-description">
          {{ (networkService.isOnline() ? 'OFFLINE.BACK_ONLINE_DESC' : 'OFFLINE.DESC') | translate }}
        </p>

        <div class="offline-actions">
          <button type="button" class="btn-retry" (click)="retry()">
            {{ 'OFFLINE.RETRY_BTN' | translate }}
          </button>
          <a routerLink="/" class="btn-home">
            {{ 'OFFLINE.HOME_BTN' | translate }}
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .offline-container {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 80vh;
        padding: 24px;
        background: #f8fafc;
      }

      .offline-card {
        background: #ffffff;
        border-radius: 16px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
        border: 1px solid #e2e8f0;
        max-width: 480px;
        width: 100%;
        padding: 40px 32px;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .offline-icon-wrapper {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: #fee2e2;
        color: #ef4444;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 24px;
        transition: all 0.3s ease;

        &.is-online {
          background: #dcfce7;
          color: #16a34a;
        }
      }

      .offline-svg {
        width: 40px;
        height: 40px;
      }

      .offline-title {
        font-size: 22px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 12px;
      }

      .offline-description {
        font-size: 14px;
        color: #64748b;
        line-height: 1.6;
        margin: 0 0 28px;
      }

      .offline-actions {
        display: flex;
        flex-direction: column;
        gap: 12px;
        width: 100%;
      }

      .btn-retry {
        background: #3f51b5;
        color: #ffffff;
        border: none;
        padding: 12px 24px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.2s ease;

        &:hover {
          background: #303f9f;
        }
      }

      .btn-home {
        background: transparent;
        color: #64748b;
        border: 1px solid #cbd5e1;
        padding: 10px 24px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 600;
        text-decoration: none;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: #f1f5f9;
          color: #1e293b;
        }
      }
    `,
  ],
})
export class OfflineComponent {
  readonly networkService = inject(NetworkService);
  private readonly router = inject(Router);

  retry(): void {
    if (this.networkService.checkConnection()) {
      void this.router.navigate(['/']);
    }
  }
}
