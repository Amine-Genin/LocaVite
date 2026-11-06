import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { PwaInstallPromptComponent } from './pwa-install-prompt.component';
import { PwaInstallService, BeforeInstallPromptEvent } from '../../../core/services/pwa-install.service';

describe('PwaInstallPromptComponent', () => {
  let component: PwaInstallPromptComponent;
  let fixture: ComponentFixture<PwaInstallPromptComponent>;
  let pwaService: PwaInstallService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PwaInstallPromptComponent, TranslateModule.forRoot()],
      providers: [PwaInstallService],
    }).compileComponents();

    fixture = TestBed.createComponent(PwaInstallPromptComponent);
    component = fixture.componentInstance;
    pwaService = TestBed.inject(PwaInstallService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render banner when canInstall is false', () => {
    pwaService.setDeferredPromptForTesting(null);
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.pwa-install-card');
    expect(banner).toBeNull();
  });

  it('should render banner when canInstall is true', () => {
    const mockPrompt = {
      prompt: jasmine.createSpy('prompt').and.returnValue(Promise.resolve()),
      userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    pwaService.setDeferredPromptForTesting(mockPrompt);
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.pwa-install-card');
    expect(banner).not.toBeNull();
  });

  it('should delegate install to pwaService.promptInstall()', () => {
    spyOn(pwaService, 'promptInstall').and.returnValue(Promise.resolve(true));

    component.install();
    expect(pwaService.promptInstall).toHaveBeenCalled();
  });

  it('should delegate dismiss to pwaService.dismiss()', () => {
    spyOn(pwaService, 'dismiss');

    component.dismiss();
    expect(pwaService.dismiss).toHaveBeenCalled();
  });
});
