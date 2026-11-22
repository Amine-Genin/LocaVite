import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

import { NavbarComponent } from './navbar.component';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let isLoggedInSignal: ReturnType<typeof signal<boolean>>;
  let authServiceMock: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    isLoggedInSignal = signal(false);
    authServiceMock = jasmine.createSpyObj('AuthService', ['hasRole', 'hasAnyRole', 'logout'], {
      isLoggedIn: isLoggedInSignal,
      currentUser: signal(null),
    });
    authServiceMock.hasRole.and.returnValue(false);
    authServiceMock.hasAnyRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [NavbarComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        {
          provide: LanguageService,
          useValue: {
            currentLang: () => 'fr',
            setLanguage: jasmine.createSpy('setLanguage'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show catalogue and sale links when not authenticated', () => {
    expect(component.navLinks().map((l) => l.route)).toEqual(['/vehicles', '/sale']);
  });

  it('should show client links when logged in as CLIENT', () => {
    isLoggedInSignal.set(true);
    authServiceMock.hasRole.and.callFake((role: string) => role === 'CLIENT');

    expect(component.navLinks().map((l) => l.route)).toEqual([
      '/vehicles',
      '/sale',
      '/my-reservations',
      '/my-payments',
    ]);
  });

  it('should show admin/agency links when logged in as AGENCE or ADMIN', () => {
    isLoggedInSignal.set(true);
    authServiceMock.hasAnyRole.and.returnValue(true);

    expect(component.navLinks().map((l) => l.route)).toEqual([
      '/admin/vehicles',
      '/admin/vehicles/new',
      '/admin/sale',
      '/admin/sale/new',
      '/admin/reservations',
      '/admin/payments',
      '/vehicles',
      '/sale',
    ]);
  });

  it('should call auth.logout on onLogout()', () => {
    component.onLogout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
