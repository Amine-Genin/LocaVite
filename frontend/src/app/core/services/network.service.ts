import { Injectable, signal, NgZone, inject } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class NetworkService {
  private readonly ngZone = inject(NgZone);
  readonly isOnline = signal<boolean>(typeof navigator !== 'undefined' ? navigator.onLine : true);

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.ngZone.run(() => this.isOnline.set(true));
      });
      window.addEventListener('offline', () => {
        this.ngZone.run(() => this.isOnline.set(false));
      });
    }
  }

  checkConnection(): boolean {
    const status = typeof navigator !== 'undefined' ? navigator.onLine : true;
    this.isOnline.set(status);
    return status;
  }
}
