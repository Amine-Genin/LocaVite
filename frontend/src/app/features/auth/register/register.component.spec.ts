import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { RegisterComponent } from './register.component';
import { AuthService, UserResponse } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;
  let snackBarSpy: jasmine.Spy;

  const mockUserResponse: UserResponse = {
    id: 1,
    nom: 'Benali',
    prenom: 'Ahmed',
    email: 'ahmed.benali@locavite.ma',
    telephone: '0612345678',
    roles: ['CLIENT'],
    createdAt: '2026-01-15T10:00:00',
  };

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['register'], {
      accessToken: null,
    });

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, TranslateModule.forRoot()],
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
        REGISTER: {
          SUCCESS: 'Votre compte a été créé avec succès !',
          ERROR_EMAIL_EXISTS: 'Cette adresse e-mail est déjà utilisée.',
          ERROR_GENERIC: "Une erreur est survenue lors de l'inscription.",
        },
      },
    });
    translate.use('fr');

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(RegisterComponent);
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

  it('should validate strong password: min 8 characters, 1 uppercase, 1 digit', () => {
    const passwordControl = component.form.controls.password;

    // Moins de 8 caractères
    passwordControl.setValue('Pass1');
    expect(passwordControl.hasError('strongPassword')).toBeTrue();

    // Pas de majuscule
    passwordControl.setValue('password123');
    expect(passwordControl.hasError('strongPassword')).toBeTrue();

    // Pas de chiffre
    passwordControl.setValue('PasswordOnly');
    expect(passwordControl.hasError('strongPassword')).toBeTrue();

    // Valide
    passwordControl.setValue('Password123');
    expect(passwordControl.valid).toBeTrue();
  });

  it('should validate password confirmation matches password', () => {
    component.form.controls.password.setValue('Password123');
    component.form.controls.confirmPassword.setValue('DifferentPassword');

    expect(component.form.controls.confirmPassword.hasError('passwordMismatch')).toBeTrue();

    component.form.controls.confirmPassword.setValue('Password123');
    expect(component.form.controls.confirmPassword.hasError('passwordMismatch')).toBeFalse();
  });

  it('should validate Moroccan phone format', () => {
    const phoneControl = component.form.controls.telephone;

    phoneControl.setValue('12345');
    expect(phoneControl.hasError('invalidPhone')).toBeTrue();

    phoneControl.setValue('0612345678');
    expect(phoneControl.valid).toBeTrue();

    phoneControl.setValue('+212612345678');
    expect(phoneControl.valid).toBeTrue();
  });

  it('should enable submit button when all fields are valid', () => {
    component.form.patchValue({
      nom: 'Benali',
      prenom: 'Ahmed',
      email: 'ahmed.benali@locavite.ma',
      telephone: '0612345678',
      password: 'Password123',
      confirmPassword: 'Password123',
    });
    fixture.detectChanges();

    expect(component.form.valid).toBeTrue();
    const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitBtn.disabled).toBeFalse();
  });

  it('should call auth.register on submit and redirect to /login with success message', () => {
    authServiceSpy.register.and.returnValue(of(mockUserResponse));

    component.form.patchValue({
      nom: 'Benali',
      prenom: 'Ahmed',
      email: 'ahmed.benali@locavite.ma',
      telephone: '0612345678',
      password: 'Password123',
      confirmPassword: 'Password123',
    });

    component.submit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      nom: 'Benali',
      prenom: 'Ahmed',
      email: 'ahmed.benali@locavite.ma',
      telephone: '0612345678',
      password: 'Password123',
    });

    expect(snackBarSpy).toHaveBeenCalledWith(
      'Votre compte a été créé avec succès !',
      'OK',
      { duration: 5000 },
    );
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { registered: 'true' },
    });
  });

  it('should display error message in snackbar when email is already in use (409)', () => {
    authServiceSpy.register.and.returnValue(
      throwError(() => ({
        status: 409,
        error: { message: 'Cette adresse e-mail est déjà utilisée.' },
      })),
    );

    component.form.patchValue({
      nom: 'Benali',
      prenom: 'Ahmed',
      email: 'existing@locavite.ma',
      telephone: '0612345678',
      password: 'Password123',
      confirmPassword: 'Password123',
    });

    component.submit();

    expect(authServiceSpy.register).toHaveBeenCalled();
    expect(snackBarSpy).toHaveBeenCalledWith(
      'Cette adresse e-mail est déjà utilisée.',
      'OK',
      { duration: 4000 },
    );
    expect(component.loading()).toBeFalse();
  });
});

