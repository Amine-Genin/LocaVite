import { Injectable, signal, computed, NgZone, inject } from '@angular/core';

export interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed';
    platform: string;
  }>;
  prompt(): Promise<void>;
}

@Injectable({
  providedIn: 'root',
})
export class PwaInstallService {
  private readonly ngZone = inject(NgZone);
  private deferredPrompt = signal<BeforeInstallPromptEvent | null>(null);

  readonly isDismissed = signal<boolean>(false);
  readonly isInstalled = signal<boolean>(false);

  readonly canInstall = computed<boolean>(() => {
    return !this.isDismissed() && !this.isInstalled() && this.deferredPrompt() !== null;
  });

  constructor() {
    this.checkStandaloneMode();
    this.registerEventListeners();
  }

  private checkStandaloneMode(): void {
    if (typeof window !== 'undefined') {
      const isStandalone =
        window.matchMedia('(display-mode: standalone)').matches ||
        (navigator as unknown as { standalone?: boolean }).standalone === true;

      if (isStandalone) {
        this.isInstalled.set(true);
      }
    }
  }

  private registerEventListeners(): void {
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeinstallprompt', (event: Event) => {
        event.preventDefault();
        this.ngZone.run(() => {
          this.deferredPrompt.set(event as BeforeInstallPromptEvent);
        });
      });

      window.addEventListener('appinstalled', () => {
        this.ngZone.run(() => {
          this.isInstalled.set(true);
          this.deferredPrompt.set(null);
        });
      });
    }
  }

  async promptInstall(): Promise<boolean> {
    const prompt = this.deferredPrompt();
    if (!prompt) {
      return false;
    }

    try {
      await prompt.prompt();
      const choice = await prompt.userChoice;
      const accepted = choice.outcome === 'accepted';
      this.ngZone.run(() => {
        if (accepted) {
          this.isInstalled.set(true);
        }
        this.deferredPrompt.set(null);
      });
      return accepted;
    } catch {
      this.deferredPrompt.set(null);
      return false;
    }
  }

  dismiss(): void {
    this.isDismissed.set(true);
  }

  // Helper for testing
  setDeferredPromptForTesting(prompt: BeforeInstallPromptEvent | null): void {
    this.deferredPrompt.set(prompt);
  }
}
