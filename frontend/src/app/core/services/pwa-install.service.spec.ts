import { TestBed } from '@angular/core/testing';
import { PwaInstallService, BeforeInstallPromptEvent } from './pwa-install.service';

describe('PwaInstallService', () => {
  let service: PwaInstallService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PwaInstallService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should not allow install by default when no prompt event has fired', () => {
    expect(service.canInstall()).toBeFalse();
  });

  it('should allow install when beforeinstallprompt is set', () => {
    const mockPrompt = {
      preventDefault: jasmine.createSpy('preventDefault'),
      prompt: jasmine.createSpy('prompt').and.returnValue(Promise.resolve()),
      userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    service.setDeferredPromptForTesting(mockPrompt);
    expect(service.canInstall()).toBeTrue();
  });

  it('should dismiss install prompt when dismiss() is called', () => {
    const mockPrompt = {
      prompt: jasmine.createSpy('prompt').and.returnValue(Promise.resolve()),
      userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    service.setDeferredPromptForTesting(mockPrompt);
    expect(service.canInstall()).toBeTrue();

    service.dismiss();
    expect(service.isDismissed()).toBeTrue();
    expect(service.canInstall()).toBeFalse();
  });

  it('should handle accepted installation through promptInstall()', async () => {
    const mockPrompt = {
      prompt: jasmine.createSpy('prompt').and.returnValue(Promise.resolve()),
      userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    service.setDeferredPromptForTesting(mockPrompt);

    const result = await service.promptInstall();
    expect(result).toBeTrue();
    expect(mockPrompt.prompt).toHaveBeenCalled();
    expect(service.isInstalled()).toBeTrue();
    expect(service.canInstall()).toBeFalse();
  });

  it('should handle dismissed installation through promptInstall()', async () => {
    const mockPrompt = {
      prompt: jasmine.createSpy('prompt').and.returnValue(Promise.resolve()),
      userChoice: Promise.resolve({ outcome: 'dismissed', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    service.setDeferredPromptForTesting(mockPrompt);

    const result = await service.promptInstall();
    expect(result).toBeFalse();
    expect(mockPrompt.prompt).toHaveBeenCalled();
    expect(service.isInstalled()).toBeFalse();
  });

  it('should return false if promptInstall() is called without deferredPrompt', async () => {
    service.setDeferredPromptForTesting(null);
    const result = await service.promptInstall();
    expect(result).toBeFalse();
  });

  it('should handle error when prompt.prompt() throws', async () => {
    const mockPrompt = {
      prompt: jasmine.createSpy('prompt').and.rejectWith(new Error('Prompt error')),
      userChoice: Promise.resolve({ outcome: 'dismissed', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    service.setDeferredPromptForTesting(mockPrompt);

    const result = await service.promptInstall();
    expect(result).toBeFalse();
    expect(service.canInstall()).toBeFalse();
  });
});
