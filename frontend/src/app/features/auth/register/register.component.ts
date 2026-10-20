import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../../core/auth/auth.service';
import {
  strongPasswordValidator,
  moroccanPhoneValidator,
  passwordMatchValidator,
} from '../../../core/auth/auth.validators';
import { AuthHeaderComponent } from '../../../shared/components/auth-header/auth-header.component';
import { AuthPromoCardComponent } from '../../../shared/components/auth-promo-card/auth-promo-card.component';
import { AuthFooterComponent } from '../../../shared/components/auth-footer/auth-footer.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TranslateModule,
    AuthHeaderComponent,
    AuthPromoCardComponent,
    AuthFooterComponent,
  ],
  template: `
    <div class="auth-page">
      <app-auth-header></app-auth-header>

      <main class="page-container">
        <!-- Big Page Title -->
        <h1 class="page-headline">{{ 'AUTH.REGISTER.PAGE_TITLE' | translate }}</h1>

        <!-- Two Columns Grid -->
        <div class="auth-grid">
          <!-- Left Column: Form & Security Box -->
          <div class="form-column">
            <section class="card form-card">
              <h2 class="card-title">{{ 'AUTH.REGISTER.TITLE' | translate }}</h2>

              <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
                <!-- Prénom & Nom côte à côte -->
                <div class="name-grid">
                  <!-- Prénom -->
                  <div class="input-group">
                    <label for="prenom" class="input-label">{{ 'AUTH.REGISTER.PRENOM' | translate }}</label>
                    <div
                      class="input-wrapper"
                      [class.invalid]="form.controls.prenom.invalid && (form.controls.prenom.touched || form.controls.prenom.dirty)"
                    >
                      <input
                        id="prenom"
                        type="text"
                        formControlName="prenom"
                        autocomplete="given-name"
                        placeholder="Yasmine"
                        class="custom-input"
                      />
                      <mat-icon class="input-icon">person_outline</mat-icon>
                    </div>
                    @if (form.controls.prenom.invalid && (form.controls.prenom.touched || form.controls.prenom.dirty)) {
                      <span class="field-error">
                        {{ 'AUTH.REGISTER.PRENOM_REQUIRED' | translate }}
                      </span>
                    }
                  </div>

                  <!-- Nom -->
                  <div class="input-group">
                    <label for="nom" class="input-label">{{ 'AUTH.REGISTER.NOM' | translate }}</label>
                    <div
                      class="input-wrapper"
                      [class.invalid]="form.controls.nom.invalid && (form.controls.nom.touched || form.controls.nom.dirty)"
                    >
                      <input
                        id="nom"
                        type="text"
                        formControlName="nom"
                        autocomplete="family-name"
                        placeholder="Amrani"
                        class="custom-input"
                      />
                      <mat-icon class="input-icon">person_outline</mat-icon>
                    </div>
                    @if (form.controls.nom.invalid && (form.controls.nom.touched || form.controls.nom.dirty)) {
                      <span class="field-error">
                        {{ 'AUTH.REGISTER.NOM_REQUIRED' | translate }}
                      </span>
                    }
                  </div>
                </div>

                <!-- Téléphone -->
                <div class="input-group">
                  <label for="telephone" class="input-label">{{ 'AUTH.REGISTER.TELEPHONE' | translate }}</label>
                  <div
                    class="input-wrapper"
                    [class.invalid]="form.controls.telephone.invalid && (form.controls.telephone.touched || form.controls.telephone.dirty)"
                  >
                    <input
                      id="telephone"
                      type="tel"
                      formControlName="telephone"
                      autocomplete="tel"
                      placeholder="+212 600 000 014"
                      class="custom-input"
                    />
                    <mat-icon class="input-icon">phone</mat-icon>
                  </div>
                  @if (form.controls.telephone.invalid && (form.controls.telephone.touched || form.controls.telephone.dirty)) {
                    <span class="field-error">
                      @if (form.controls.telephone.hasError('required')) {
                        {{ 'AUTH.REGISTER.TELEPHONE_REQUIRED' | translate }}
                      } @else if (form.controls.telephone.hasError('invalidPhone')) {
                        {{ 'AUTH.REGISTER.TELEPHONE_INVALID' | translate }}
                      }
                    </span>
                  }
                </div>

                <!-- Adresse e-mail -->
                <div class="input-group">
                  <label for="email" class="input-label">{{ 'AUTH.REGISTER.EMAIL' | translate }}</label>
                  <div
                    class="input-wrapper"
                    [class.invalid]="form.controls.email.invalid && (form.controls.email.touched || form.controls.email.dirty)"
                  >
                    <input
                      id="email"
                      type="email"
                      formControlName="email"
                      autocomplete="email"
                      placeholder="yasmine@example.com"
                      class="custom-input"
                    />
                    <mat-icon class="input-icon">mail_outline</mat-icon>
                  </div>
                  @if (form.controls.email.invalid && (form.controls.email.touched || form.controls.email.dirty)) {
                    <span class="field-error">
                      @if (form.controls.email.hasError('required')) {
                        {{ 'AUTH.REGISTER.EMAIL_REQUIRED' | translate }}
                      } @else if (form.controls.email.hasError('email')) {
                        {{ 'AUTH.REGISTER.EMAIL_INVALID' | translate }}
                      }
                    </span>
                  }
                </div>

                <!-- Mot de passe -->
                <div class="input-group">
                  <label for="password" class="input-label">{{ 'AUTH.REGISTER.PASSWORD' | translate }}</label>
                  <div
                    class="input-wrapper"
                    [class.invalid]="form.controls.password.invalid && (form.controls.password.touched || form.controls.password.dirty)"
                  >
                    <input
                      id="password"
                      [type]="hidePassword() ? 'password' : 'text'"
                      formControlName="password"
                      autocomplete="new-password"
                      placeholder="••••••••••••"
                      class="custom-input"
                    />
                    <button
                      type="button"
                      class="icon-btn"
                      (click)="hidePassword.set(!hidePassword())"
                      [attr.aria-label]="hidePassword() ? 'Afficher mot de passe' : 'Masquer mot de passe'"
                    >
                      <mat-icon class="input-icon">{{ hidePassword() ? 'visibility' : 'visibility_off' }}</mat-icon>
                    </button>
                  </div>
                  <span class="password-hint">{{ 'AUTH.REGISTER.PASSWORD_HINT' | translate }}</span>
                  @if (form.controls.password.invalid && (form.controls.password.touched || form.controls.password.dirty)) {
                    <span class="field-error">
                      @if (form.controls.password.hasError('required')) {
                        {{ 'AUTH.REGISTER.PASSWORD_REQUIRED' | translate }}
                      } @else if (form.controls.password.hasError('strongPassword')) {
                        {{ 'AUTH.REGISTER.PASSWORD_STRONG' | translate }}
                      }
                    </span>
                  }
                </div>

                <!-- Confirmation mot de passe -->
                <div class="input-group">
                  <label for="confirmPassword" class="input-label">{{ 'AUTH.REGISTER.CONFIRM_PASSWORD' | translate }}</label>
                  <div
                    class="input-wrapper"
                    [class.invalid]="form.controls.confirmPassword.invalid && (form.controls.confirmPassword.touched || form.controls.confirmPassword.dirty)"
                  >
                    <input
                      id="confirmPassword"
                      [type]="hideConfirmPassword() ? 'password' : 'text'"
                      formControlName="confirmPassword"
                      autocomplete="new-password"
                      placeholder="••••••••••••"
                      class="custom-input"
                    />
                    <button
                      type="button"
                      class="icon-btn"
                      (click)="hideConfirmPassword.set(!hideConfirmPassword())"
                      [attr.aria-label]="hideConfirmPassword() ? 'Afficher confirmation' : 'Masquer confirmation'"
                    >
                      <mat-icon class="input-icon">{{ hideConfirmPassword() ? 'visibility' : 'visibility_off' }}</mat-icon>
                    </button>
                  </div>
                  @if (form.controls.confirmPassword.invalid && (form.controls.confirmPassword.touched || form.controls.confirmPassword.dirty)) {
                    <span class="field-error">
                      @if (form.controls.confirmPassword.hasError('required')) {
                        {{ 'AUTH.REGISTER.CONFIRM_REQUIRED' | translate }}
                      } @else if (form.controls.confirmPassword.hasError('passwordMismatch')) {
                        {{ 'AUTH.REGISTER.CONFIRM_MISMATCH' | translate }}
                      }
                    </span>
                  }
                </div>

                <!-- Conditions d'utilisation -->
                <div class="terms-row">
                  <mat-checkbox [checked]="true" color="primary">
                    <span class="checkbox-text">{{ 'AUTH.REGISTER.TERMS_LABEL' | translate }}</span>
                  </mat-checkbox>
                </div>

                <!-- Bouton Créer mon compte -->
                <button
                  mat-flat-button
                  type="submit"
                  class="btn-primary"
                  [disabled]="form.invalid || loading()"
                >
                  @if (loading()) {
                    <mat-progress-spinner
                      mode="indeterminate"
                      diameter="20"
                      class="btn-spinner"
                    ></mat-progress-spinner>
                    <span>{{ 'AUTH.REGISTER.SUBMITTING' | translate }}</span>
                  } @else {
                    <span>{{ 'AUTH.REGISTER.SUBMIT' | translate }}</span>
                  }
                </button>

                <!-- Bouton J'ai déjà un compte · Connexion -->
                <a routerLink="/login" class="btn-outline">
                  {{ 'AUTH.REGISTER.ALREADY_HAVE_ACCOUNT_BTN' | translate }}
                </a>
              </form>
            </section>

            <!-- Un espace sécurisé -->
            <div class="security-box">
              <h3 class="security-title">{{ 'AUTH.SECURITY.TITLE' | translate }}</h3>
              <p class="security-desc">{{ 'AUTH.SECURITY.DESC' | translate }}</p>
            </div>
          </div>

          <!-- Right Column: Promo Card -->
          <div class="promo-column">
            <app-auth-promo-card></app-auth-promo-card>
          </div>
        </div>
      </main>

      <app-auth-footer></app-auth-footer>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: #f8fafc;
      font-family: 'Inter', sans-serif;
    }

    .page-container {
      flex: 1;
      max-width: 1360px;
      width: 100%;
      margin: 0 auto;
      padding: 40px 32px 20px 32px;
      box-sizing: border-box;
    }

    .page-headline {
      font-size: 38px;
      font-weight: 700;
      color: var(--c-navy);
      letter-spacing: -0.5px;
      margin: 0 0 36px 0;
      line-height: 1.2;
    }

    .auth-grid {
      display: grid;
      grid-template-columns: minmax(0, 520px) minmax(0, 560px);
      justify-content: space-between;
      gap: 48px;
      align-items: start;
    }

    .form-column {
      width: 100%;
    }

    .promo-column {
      width: 100%;
    }

    .form-card {
      background: #ffffff;
      border: 1px solid #e5edf5;
      border-radius: 18px;
      padding: 36px;
      box-shadow: 0 4px 20px rgba(20, 46, 69, 0.04);
    }

    .card-title {
      font-size: 20px;
      font-weight: 700;
      color: var(--c-navy);
      margin: 0 0 28px 0;
    }

    form {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }

    .name-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 7px;
    }

    .input-label {
      font-size: 13px;
      font-weight: 600;
      color: var(--c-navy);
    }

    .input-wrapper {
      display: flex;
      align-items: center;
      height: 48px;
      border: 1px solid #dce4ee;
      border-radius: 10px;
      padding: 0 14px;
      background: #ffffff;
      transition: all 0.15s ease;

      &:focus-within {
        border-color: var(--c-navy);
        box-shadow: 0 0 0 1px var(--c-navy);
      }

      &.invalid {
        border-color: var(--c-error);
      }
    }

    .custom-input {
      flex: 1;
      height: 100%;
      border: none;
      outline: none;
      background: transparent;
      font-size: 15px;
      color: var(--c-navy);
      font-family: inherit;

      &::placeholder {
        color: #9ab0c2;
      }
    }

    .input-icon {
      color: #7b8e9f;
      font-size: 20px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .icon-btn {
      background: none;
      border: none;
      padding: 0;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .password-hint {
      font-size: 12px;
      color: #7b8e9f;
      margin-top: 2px;
    }

    .field-error {
      color: var(--c-error);
      font-size: 12px;
      font-weight: 500;
      margin-top: 2px;
    }

    .terms-row {
      display: flex;
      align-items: center;
      margin: 4px 0 6px 0;
    }

    .checkbox-text {
      font-size: 12.5px;
      color: #3b5266;
      font-weight: 500;
      line-height: 1.4;
    }

    .btn-primary {
      width: 100%;
      height: 48px;
      background: var(--c-primary) !important;
      color: #ffffff !important;
      font-size: 15px;
      font-weight: 600;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      border: none;
      cursor: pointer;
      transition: background 0.15s ease;

      &:hover:not([disabled]) {
        background: var(--c-primary-dark) !important;
      }

      &[disabled] {
        opacity: 0.55;
        cursor: not-allowed;
      }
    }

    .btn-spinner {
      display: inline-block;
    }

    .btn-outline {
      width: 100%;
      height: 48px;
      border: 1px solid #dce4ee;
      background: #ffffff;
      color: var(--c-navy);
      font-size: 15px;
      font-weight: 600;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      text-decoration: none;
      box-sizing: border-box;
      transition: all 0.15s ease;

      &:hover {
        background: #f5f8fb;
        border-color: #cbd8e5;
      }
    }

    .security-box {
      margin-top: 24px;
      background: #eef5fc;
      border-radius: 12px;
      padding: 20px 22px;
    }

    .security-title {
      font-size: 14px;
      font-weight: 700;
      color: var(--c-navy);
      margin: 0 0 6px 0;
    }

    .security-desc {
      font-size: 13px;
      line-height: 1.55;
      color: #4b6276;
      margin: 0;
    }

    @media (max-width: 1024px) {
      .auth-grid {
        grid-template-columns: 1fr;
        max-width: 600px;
        margin: 0 auto;
      }
      .page-headline {
        font-size: 30px;
        margin-bottom: 24px;
      }
    }

    @media (max-width: 600px) {
      .name-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 480px) {
      .page-container {
        padding: 24px 16px;
      }
      .form-card {
        padding: 24px 20px;
      }
    }
  `],
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(false);
  hidePassword = signal(true);
  hideConfirmPassword = signal(true);

  form = this.fb.nonNullable.group(
    {
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.required, moroccanPhoneValidator]],
      password: ['', [Validators.required, strongPasswordValidator]],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: [passwordMatchValidator('password', 'confirmPassword')],
    },
  );

  ngOnInit() {
    if (this.auth.accessToken) {
      this.router.navigate(['/dashboard']);
    }
  }

  submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const { nom, prenom, email, telephone, password } = this.form.getRawValue();

    this.auth.register({ nom, prenom, email, telephone, password }).subscribe({
      next: () => {
        this.loading.set(false);
        const successMsg =
          this.translate.instant('AUTH.REGISTER.SUCCESS') ||
          'Compte créé avec succès ! Connectez-vous.';
        this.snackBar.open(successMsg, 'OK', { duration: 5000 });
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.loading.set(false);
        let errorMsg = err?.error?.message;
        if (!errorMsg) {
          if (err?.status === 409) {
            errorMsg =
              this.translate.instant('AUTH.REGISTER.ERROR_EMAIL_EXISTS') ||
              'Cette adresse e-mail est déjà utilisée.';
          } else {
            errorMsg =
              this.translate.instant('AUTH.REGISTER.ERROR_GENERIC') ||
              "Une erreur est survenue lors de l'inscription.";
          }
        }
        this.snackBar.open(errorMsg, 'OK', { duration: 4000 });
      },
    });
  }
}
