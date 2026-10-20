import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { LoginComponent } from './login.component';
import { AuthService, AuthResponse } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  let snackBarSpy: jasmine.Spy;

  const mockAuthResponse: AuthResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    expiresIn: 3600,
    user: {
      id: 1,
      nom: 'Benali',
      prenom: 'Ahmed',
      email: 'ahmed.benali@locavite.ma',
      telephone: '0612345678',
      roles: ['CLIENT'],
      createdAt: '2026-01-15T10:00:00',
    },
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login'], {
      accessToken: null,
      rememberedEmail: '',
    });

    await TestBed.configureTestingModule({
      imports: [LoginComponent, TranslateModule.forRoot()],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: LanguageService,
          useValue: {
            currentLang: () => 'fr',
            setLanguage: jasmine.createSpy('setLanguage'),
          },
        },
      ],
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('fr', {
      AUTH: {
        LOGIN: {
          ERROR: 'Identifiants incorrects',
          SUCCESS_AFTER_REGISTER: 'Compte créé avec succès ! Veuillez vous connecter.',
        },
      },
    });
    translate.use('fr');

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    snackBarSpy = spyOn((component as any).snackBar, 'open').and.callThrough();
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should have submit button disabled when form is invalid', () => {
    expect(component.form.valid).toBeFalse();

    const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitBtn.disabled).toBeTrue();
  });

  it('should enable submit button when form is valid', () => {
    component.form.controls.email.setValue('test@locavite.ma');
    component.form.controls.password.setValue('Password123');
    fixture.detectChanges();

    expect(component.form.valid).toBeTrue();
    const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitBtn.disabled).toBeFalse();
  });

  it('should call auth.login on submit and navigate to /dashboard on success', () => {
    authServiceSpy.login.and.returnValue(of(mockAuthResponse));

    component.form.controls.email.setValue('ahmed.benali@locavite.ma');
    component.form.controls.password.setValue('Password123');
    component.form.controls.rememberMe.setValue(true);

    component.submit();

    expect(authServiceSpy.login).toHaveBeenCalledWith(
      'ahmed.benali@locavite.ma',
      'Password123',
      true,
    );
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should display error message in snackBar when login fails', () => {
    authServiceSpy.login.and.returnValue(
      throwError(() => ({
        error: { message: 'Identifiants incorrects' },
      })),
    );

    component.form.controls.email.setValue('wrong@locavite.ma');
    component.form.controls.password.setValue('WrongPassword');

    component.submit();

    expect(authServiceSpy.login).toHaveBeenCalled();
    expect(snackBarSpy).toHaveBeenCalledWith('Identifiants incorrects', 'OK', {
      duration: 4000,
    });
    expect(component.loading()).toBeFalse();
  });

  it('should toggle password visibility when button clicked', () => {
    expect(component.hidePassword()).toBeTrue();
    component.hidePassword.set(false);
    expect(component.hidePassword()).toBeFalse();
  });
});

