import { Component, Input } from '@angular/core';

export type BadgeTone = 'success' | 'warning' | 'error' | 'info';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="tone"><ng-content /></span>`,
  styles: [`
    .badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
    }
    .success { color: var(--c-success); background: var(--c-success-bg); }
    .warning { color: var(--c-warning); background: var(--c-warning-bg); }
    .error   { color: var(--c-error);   background: var(--c-error-bg); }
    .info    { color: var(--c-info);    background: var(--c-info-bg); }
  `],
})
export class StatusBadgeComponent {
  @Input() tone: BadgeTone = 'info';
}