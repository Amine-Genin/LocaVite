import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { LanguageSwitcherComponent } from './language-switcher.component';
import { LanguageService } from '../../../core/i18n/language.service';

describe('LanguageSwitcherComponent', () => {
  let component: LanguageSwitcherComponent;
  let fixture: ComponentFixture<LanguageSwitcherComponent>;
  let langService: LanguageService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [LanguageSwitcherComponent, TranslateModule.forRoot()],
      providers: [provideNoopAnimations(), LanguageService],
    }).compileComponents();

    langService = TestBed.inject(LanguageService);
    fixture = TestBed.createComponent(LanguageSwitcherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render button variant and change language on click', () => {
    component.variant = 'buttons';
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('.lang-btn');
    expect(buttons.length).toBe(3);

    const spy = spyOn(langService, 'setLanguage').and.callThrough();

    // Click AR button
    buttons[2].click();
    expect(spy).toHaveBeenCalledWith('ar');

    // Click EN button
    buttons[1].click();
    expect(spy).toHaveBeenCalledWith('en');

    // Click FR button
    buttons[0].click();
    expect(spy).toHaveBeenCalledWith('fr');
  });

  it('should render toggle variant by default', () => {
    expect(component.variant).toBe('toggle');
    const toggleGroup = fixture.nativeElement.querySelector('mat-button-toggle-group');
    expect(toggleGroup).toBeTruthy();
  });
});
