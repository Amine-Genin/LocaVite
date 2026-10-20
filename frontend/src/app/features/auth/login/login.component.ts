import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { AuthService } from '../../../core/auth/auth.service';
import { AuthHeaderComponent } from '../../../shared/components/auth-header/auth-header.component';
import { AuthPromoCardComponent } from '../../../shared/components/auth-promo-card/auth-promo-card.component';
import { AuthFooterComponent } from '../../../shared/components/auth-footer/auth-footer.component';

@Component({
  selector: 'app-login',
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
        <h1 class="page-headline">{{ 'AUTH.LOGIN.PAGE_TITLE' | translate }}</h1>

        <!-- Two Columns Grid -->
        <div class="auth-grid">
          <!-- Left Column: Form & Security Box -->
          <div class="form-column">
            <section class="card form-card">
              <h2 class="card-title">{{ 'AUTH.LOGIN.TITLE' | translate }}</h2>

              <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
                <!-- Email -->
                <div class="input-group">
                  <label for="email" class="input-label">{{ 'AUTH.LOGIN.EMAIL' | translate }}</label>
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
                        {{ 'AUTH.LOGIN.EMAIL_REQUIRED' | translate }}
                      } @else if (form.controls.email.hasError('email')) {
                        {{ 'AUTH.LOGIN.EMAIL_INVALID' | translate }}
                      }
                    </span>
                  }
                </div>

                <!-- Password -->
                <div class="input-group">
                  <label for="password" class="input-label">{{ 'AUTH.LOGIN.PASSWORD' | translate }}</label>
                  <div
                    class="input-wrapper"
                    [class.invalid]="form.controls.password.invalid && (form.controls.password.touched || form.controls.password.dirty)"
                  >
                    <input
                      id="password"
                      [type]="hidePassword() ? 'password' : 'text'"
                      formControlName="password"
                      autocomplete="current-password"
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
                  @if (form.controls.password.invalid && (form.controls.password.touched || form.controls.password.dirty)) {
                    <span class="field-error">
                      {{ 'AUTH.LOGIN.PASSWORD_REQUIRED' | translate }}
                    </span>
                  }
                </div>

                <!-- Rester connecté sur cet appareil -->
                <div class="options-row">
                  <mat-checkbox formControlName="rememberMe" color="primary">
                    <span class="checkbox-text">{{ 'AUTH.LOGIN.REMEMBER_ME' | translate }}</span>
                  </mat-checkbox>
                </div>

                <!-- Bouton Se connecter -->
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
                    <span>{{ 'AUTH.LOGIN.SUBMITTING' | translate }}</span>
                  } @else {
                    <span>{{ 'AUTH.LOGIN.SUBMIT' | translate }}</span>
                  }
                </button>

                <!-- Mot de passe oublié ? -->
                <div class="forgot-wrapper">
                  <a href="javascript:void(0)" class="forgot-link">
                    {{ 'AUTH.LOGIN.FORGOT_PASSWORD' | translate }}
                  </a>
                </div>

                <!-- Bouton Créer un compte -->
                <a routerLink="/register" class="btn-outline">
                  {{ 'AUTH.LOGIN.REGISTER_LINK' | translate }}
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
      gap: 20px;
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

    .field-error {
      color: var(--c-error);
      font-size: 12px;
      font-weight: 500;
      margin-top: 2px;
    }

    .options-row {
      display: flex;
      align-items: center;
      margin: 4px 0 6px 0;
    }

    .checkbox-text {
      font-size: 13px;
      color: #3b5266;
      font-weight: 500;
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

    .forgot-wrapper {
      text-align: center;
      margin: 4px 0;
    }

    .forgot-link {
      font-size: 13px;
      color: var(--c-navy);
      text-decoration: none;
      font-weight: 500;

      &:hover {
        text-decoration: underline;
      }
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
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  loading = signal(false);
  hidePassword = signal(true);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  ngOnInit() {
    if (this.auth.accessToken) {
      this.router.navigate(['/dashboard']);
      return;
    }

    const savedEmail = this.auth.rememberedEmail;
    if (savedEmail) {
      this.form.patchValue({ email: savedEmail, rememberMe: true });
    }

    this.route.queryParams.subscribe((params) => {
      if (params['registered']) {
        const msg = this.translate.instant('AUTH.LOGIN.SUCCESS_AFTER_REGISTER') ||
          'Compte créé avec succès ! Veuillez vous connecter.';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }

  submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    const { email, password, rememberMe } = this.form.getRawValue();

    this.auth.login(email, password, rememberMe).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const errorMsg =
          err?.error?.message ||
          this.translate.instant('AUTH.LOGIN.ERROR') ||
          'Identifiants incorrects';
        this.snackBar.open(errorMsg, 'OK', { duration: 4000 });
      },
    });
  }
}
