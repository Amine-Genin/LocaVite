import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageSwitcherComponent } from '../language-switcher/language-switcher.component';

interface NavLinkItem {
  labelKey: string;
  route: string;
  exact?: boolean;
}

const NAV_FALLBACKS: Record<string, { NAV: Record<string, string> }> = {
  fr: {
    NAV: {
      CATALOGUE: 'Catalogue',
      BUY_CAR: 'Acheter une voiture',
      MY_RESERVATIONS: 'Mes réservations',
      MY_PAYMENTS: 'Mes paiements',
      ADMIN_VEHICLES: 'Flotte véhicules',
      ADD_VEHICLE: 'Ajouter véhicule',
      ADMIN_RESERVATIONS: 'Réservations',
      ADMIN_PAYMENTS: 'Paiements',
      PROFILE: 'Mon profil',
      LOGIN: 'Connexion',
      REGISTER: 'Inscription',
      LOGOUT: 'Déconnexion',
      MENU_TOGGLE: 'Ouvrir le menu',
      QUICK_ACCESS: 'Accès rapide',
    },
  },
  en: {
    NAV: {
      CATALOGUE: 'Vehicles',
      BUY_CAR: 'Buy a car',
      MY_RESERVATIONS: 'My Reservations',
      MY_PAYMENTS: 'My Payments',
      ADMIN_VEHICLES: 'Fleet',
      ADD_VEHICLE: 'Add Vehicle',
      ADMIN_RESERVATIONS: 'Reservations',
      ADMIN_PAYMENTS: 'Payments',
      PROFILE: 'Profile',
      LOGIN: 'Sign in',
      REGISTER: 'Register',
      LOGOUT: 'Sign out',
      MENU_TOGGLE: 'Toggle menu',
      QUICK_ACCESS: 'Quick access',
    },
  },
  ar: {
    NAV: {
      CATALOGUE: 'الكتالوج',
      BUY_CAR: 'شراء سيارة',
      MY_RESERVATIONS: 'حجوزاتي',
      MY_PAYMENTS: 'مدفوعاتي',
      ADMIN_VEHICLES: 'الأسطول',
      ADD_VEHICLE: 'إضافة سيارة',
      ADMIN_RESERVATIONS: 'الحجوزات',
      ADMIN_PAYMENTS: 'المدفوعات',
      PROFILE: 'ملفي الشخصي',
      LOGIN: 'تسجيل الدخول',
      REGISTER: 'إنشاء حساب',
      LOGOUT: 'تسجيل الخروج',
      MENU_TOGGLE: 'فتح القائمة',
      QUICK_ACCESS: 'وصول سريع',
    },
  },
};

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    LanguageSwitcherComponent,
  ],
  template: `
    <header class="navbar-header" data-cy="app-navbar">
      <div class="navbar-inner">
        <a [routerLink]="homeRoute()" class="brand" (click)="closeMenu()">
          <svg
            class="brand-icon"
            width="26"
            height="26"
            viewBox="0 0 28 28"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path d="M14 2L2.5 25H7.5L14 11.5L20.5 25H25.5L14 2Z" fill="var(--c-primary)" />
            <path d="M14 16L10.5 23H17.5L14 16Z" fill="var(--c-primary)" />
          </svg>
          <span class="brand-text">LOCAVITE</span>
        </a>

        <nav class="desktop-nav" aria-label="Main navigation">
          @for (item of navLinks(); track item.route) {
            <a
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
              class="nav-link"
            >
              {{ item.labelKey | translate }}
            </a>
          }
        </nav>

        <div class="navbar-actions">
          <app-language-switcher [variant]="'buttons'"></app-language-switcher>

          @if (auth.isLoggedIn()) {
            <a
              routerLink="/profile"
              routerLinkActive="active"
              class="profile-pill"
              data-cy="nav-profile-link"
            >
              <mat-icon class="pill-icon">account_circle</mat-icon>
              <span>{{ 'NAV.PROFILE' | translate }}</span>
            </a>
            <button
              type="button"
              mat-stroked-button
              class="logout-btn"
              (click)="onLogout()"
              data-cy="nav-logout-btn"
            >
              {{ 'NAV.LOGOUT' | translate }}
            </button>
          } @else {
            <a routerLink="/login" routerLinkActive="active" class="nav-link auth-link">
              {{ 'NAV.LOGIN' | translate }}
            </a>
            <a routerLink="/register" class="register-btn">
              {{ 'NAV.REGISTER' | translate }}
            </a>
          }

          <button
            type="button"
            class="menu-toggle"
            (click)="toggleMenu()"
            [attr.aria-expanded]="menuOpen()"
            [attr.aria-label]="'NAV.MENU_TOGGLE' | translate"
          >
            <mat-icon>{{ menuOpen() ? 'close' : 'menu' }}</mat-icon>
          </button>
        </div>
      </div>

      @if (menuOpen()) {
        <div class="mobile-drawer" data-cy="mobile-nav-drawer">
          <nav class="mobile-nav">
            @for (item of navLinks(); track item.route) {
              <a
                [routerLink]="item.route"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
                class="mobile-link"
                (click)="closeMenu()"
              >
                {{ item.labelKey | translate }}
              </a>
            }

            <div class="mobile-divider"></div>

            @if (auth.isLoggedIn()) {
              <a
                routerLink="/profile"
                routerLinkActive="active"
                class="mobile-link"
                (click)="closeMenu()"
              >
                {{ 'NAV.PROFILE' | translate }}
              </a>
              <button type="button" class="mobile-link mobile-logout" (click)="onLogout()">
                {{ 'NAV.LOGOUT' | translate }}
              </button>
            } @else {
              <a routerLink="/login" class="mobile-link" (click)="closeMenu()">
                {{ 'NAV.LOGIN' | translate }}
              </a>
              <a routerLink="/register" class="mobile-link" (click)="closeMenu()">
                {{ 'NAV.REGISTER' | translate }}
              </a>
            }
          </nav>
        </div>
      }
    </header>
  `,
  styles: [
    `
      .navbar-header {
        background: #ffffff;
        border-bottom: 1px solid #edf1f6;
        position: sticky;
        top: 0;
        z-index: 100;
      }

      .navbar-inner {
        max-width: 1360px;
        margin: 0 auto;
        padding: 12px 24px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 20px;
      }

      .brand {
        display: inline-flex;
        align-items: center;
        gap: 10px;
        text-decoration: none;
        flex-shrink: 0;
      }

      .brand-icon {
        display: block;
      }

      .brand-text {
        font-size: 20px;
        font-weight: 800;
        letter-spacing: 0.8px;
        color: var(--c-navy, #0f172a);
      }

      .desktop-nav {
        display: flex;
        align-items: center;
        gap: 20px;
        flex-wrap: wrap;
      }

      .nav-link {
        font-size: 14px;
        font-weight: 500;
        color: var(--c-navy, #0f172a);
        text-decoration: none;
        padding: 6px 10px;
        border-radius: 8px;
        transition: color 0.15s ease, background-color 0.15s ease;

        &:hover {
          color: var(--c-primary, #1d4ed8);
          background: #f1f5f9;
        }

        &.active {
          color: var(--c-primary, #1d4ed8);
          background: rgba(29, 78, 216, 0.08);
          font-weight: 600;
        }
      }

      .navbar-actions {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;
      }

      .profile-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 6px 12px;
        border-radius: 999px;
        font-size: 13px;
        font-weight: 600;
        color: var(--c-navy, #0f172a);
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        text-decoration: none;
        transition: all 0.15s ease;

        &:hover,
        &.active {
          border-color: var(--c-primary, #1d4ed8);
          color: var(--c-primary, #1d4ed8);
        }
      }

      .pill-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      .register-btn {
        display: inline-flex;
        align-items: center;
        padding: 8px 16px;
        border-radius: 8px;
        font-size: 13px;
        font-weight: 600;
        text-decoration: none;
        color: #ffffff;
        background: var(--c-primary, #1d4ed8);
        transition: opacity 0.15s ease;

        &:hover {
          opacity: 0.9;
        }
      }

      .logout-btn {
        font-size: 13px;
      }

      .menu-toggle {
        display: none;
        background: transparent;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 6px;
        cursor: pointer;
        color: var(--c-navy, #0f172a);
        align-items: center;
        justify-content: center;
      }

      .mobile-drawer {
        display: none;
        border-top: 1px solid #edf1f6;
        background: #ffffff;
        padding: 12px 24px 16px;
      }

      .mobile-nav {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }

      .mobile-link {
        display: block;
        text-align: start;
        padding: 10px 12px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        color: var(--c-navy, #0f172a);
        text-decoration: none;
        background: transparent;
        border: none;
        cursor: pointer;

        &.active {
          color: var(--c-primary, #1d4ed8);
          background: rgba(29, 78, 216, 0.08);
          font-weight: 600;
        }
      }

      .mobile-logout {
        color: #dc2626;
      }

      .mobile-divider {
        height: 1px;
        background: #edf1f6;
        margin: 6px 0;
      }

      @media (max-width: 960px) {
        .desktop-nav,
        .profile-pill,
        .logout-btn,
        .auth-link,
        .register-btn {
          display: none;
        }

        .menu-toggle {
          display: inline-flex;
        }

        .mobile-drawer {
          display: block;
        }
      }
    `,
  ],
})
export class NavbarComponent {
  readonly auth = inject(AuthService);
  private readonly translate = inject(TranslateService, { optional: true });
  readonly menuOpen = signal(false);

  constructor() {
    if (this.translate) {
      for (const [lang, dict] of Object.entries(NAV_FALLBACKS)) {
        this.translate.setTranslation(lang, dict, true);
      }
    }
  }

  readonly isAdminOrAgency = computed(() => {
    this.auth.isLoggedIn();
    this.auth.currentUser();
    return this.auth.hasAnyRole(['AGENCE', 'ADMIN']);
  });

  readonly isClient = computed(() => {
    this.auth.isLoggedIn();
    this.auth.currentUser();
    return this.auth.hasRole('CLIENT');
  });

  readonly homeRoute = computed(() => (this.isAdminOrAgency() ? '/admin/reservations' : '/vehicles'));

  readonly navLinks = computed<NavLinkItem[]>(() => {
    if (!this.auth.isLoggedIn()) {
      return [
        { labelKey: 'NAV.CATALOGUE', route: '/vehicles', exact: false },
        { labelKey: 'NAV.BUY_CAR', route: '/sale', exact: true },
      ];
    }

    if (this.isAdminOrAgency()) {
      return [
        { labelKey: 'NAV.ADMIN_VEHICLES', route: '/admin/vehicles', exact: true },
        { labelKey: 'NAV.ADD_VEHICLE', route: '/admin/vehicles/new', exact: true },
        { labelKey: 'NAV.ADMIN_RESERVATIONS', route: '/admin/reservations', exact: true },
        { labelKey: 'NAV.ADMIN_PAYMENTS', route: '/admin/payments', exact: true },
        { labelKey: 'NAV.CATALOGUE', route: '/vehicles', exact: false },
        { labelKey: 'NAV.BUY_CAR', route: '/sale', exact: true },
      ];
    }

    if (this.isClient()) {
      return [
        { labelKey: 'NAV.CATALOGUE', route: '/vehicles', exact: false },
        { labelKey: 'NAV.BUY_CAR', route: '/sale', exact: true },
        { labelKey: 'NAV.MY_RESERVATIONS', route: '/my-reservations', exact: true },
        { labelKey: 'NAV.MY_PAYMENTS', route: '/my-payments', exact: true },
      ];
    }

    return [
      { labelKey: 'NAV.CATALOGUE', route: '/vehicles', exact: false },
      { labelKey: 'NAV.BUY_CAR', route: '/sale', exact: true },
    ];
  });

  toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  onLogout(): void {
    this.closeMenu();
    this.auth.logout();
  }
}
