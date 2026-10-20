import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import {
  AuthService,
  AuthResponse,
  ACCESS_KEY,
  REFRESH_KEY,
  USER_KEY,
  REMEMBER_EMAIL_KEY,
  decodeJwt,
  isTokenExpired,
} from './auth.service';

function createFakeJwt(payload: Record<string, unknown>): string {
  const b64 = (obj: unknown) =>
    btoa(unescape(encodeURIComponent(JSON.stringify(obj))))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.fake-signature`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockUser = {
    id: 1,
    nom: 'Benani',
    prenom: 'Sami',
    email: 'sami@locavite.ma',
    telephone: '+212611223344',
    roles: ['CLIENT'],
    createdAt: '2026-01-01',
  };

  const validToken = createFakeJwt({
    sub: 'sami@locavite.ma',
    roles: ['CLIENT'],
    exp: Math.floor(Date.now() / 1000) + 3600,
  });

  const expiredToken = createFakeJwt({
    sub: 'sami@locavite.ma',
    roles: ['CLIENT'],
    exp: Math.floor(Date.now() / 1000) - 3600,
  });

  beforeEach(() => {
    localStorage.clear();
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    service.stopAutoRefresh();
  });

  it('devrait être créé', () => {
    expect(service).toBeTruthy();
  });

  describe('Stockage & isAuthenticated & getToken', () => {
    it('devrait retourner null pour getToken() si aucun token n\'est stocké', () => {
      expect(service.getToken()).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('devrait retourner true pour isAuthenticated() avec un token valide', () => {
      localStorage.setItem(ACCESS_KEY, validToken);
      expect(service.getToken()).toBe(validToken);
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('devrait retourner false pour isAuthenticated() avec un token expiré', () => {
      localStorage.setItem(ACCESS_KEY, expiredToken);
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  describe('login()', () => {
    it('devrait stocker accessToken, refreshToken et user dans localStorage', () => {
      const mockResponse: AuthResponse = {
        accessToken: validToken,
        refreshToken: 'refresh-abc-123',
        expiresIn: 3600,
        user: mockUser,
      };

      service.login('sami@locavite.ma', 'Secret123!', true).subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'sami@locavite.ma', password: 'Secret123!' });
      req.flush(mockResponse);

      expect(localStorage.getItem(ACCESS_KEY)).toBe(validToken);
      expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-abc-123');
      expect(localStorage.getItem(USER_KEY)).toContain('sami@locavite.ma');
      expect(localStorage.getItem(REMEMBER_EMAIL_KEY)).toBe('sami@locavite.ma');
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('devrait supprimer REMEMBER_EMAIL si rememberMe est faux', () => {
      localStorage.setItem(REMEMBER_EMAIL_KEY, 'old@locavite.ma');
      const mockResponse: AuthResponse = {
        accessToken: validToken,
        refreshToken: 'refresh-abc-123',
        expiresIn: 3600,
        user: mockUser,
      };

      service.login('sami@locavite.ma', 'Secret123!', false).subscribe();

      const req = httpMock.expectOne('/api/auth/login');
      req.flush(mockResponse);

      expect(localStorage.getItem(REMEMBER_EMAIL_KEY)).toBeNull();
    });
  });

  describe('refreshToken()', () => {
    it('devrait rafraîchir les tokens via l\'API /api/auth/refresh', () => {
      localStorage.setItem(REFRESH_KEY, 'existing-refresh-token');

      const newAccessToken = createFakeJwt({
        sub: 'sami@locavite.ma',
        roles: ['CLIENT'],
        exp: Math.floor(Date.now() / 1000) + 7200,
      });

      const refreshResponse: AuthResponse = {
        accessToken: newAccessToken,
        refreshToken: 'renewed-refresh-token',
        expiresIn: 7200,
        user: mockUser,
      };

      service.refreshToken().subscribe((res) => {
        expect(res.accessToken).toBe(newAccessToken);
      });

      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'existing-refresh-token' });
      req.flush(refreshResponse);

      expect(localStorage.getItem(ACCESS_KEY)).toBe(newAccessToken);
      expect(localStorage.getItem(REFRESH_KEY)).toBe('renewed-refresh-token');
    });

    it('devrait lever une erreur si aucun refresh token n\'est disponible', (done) => {
      service.refreshToken().subscribe({
        error: (err) => {
          expect(err).toBeTruthy();
          done();
        },
      });
    });
  });

  describe('logout()', () => {
    it('devrait révoquer le token auprès du backend, vider le localStorage et rediriger vers /login', () => {
      localStorage.setItem(ACCESS_KEY, validToken);
      localStorage.setItem(REFRESH_KEY, 'refresh-xyz');
      localStorage.setItem(USER_KEY, JSON.stringify(mockUser));

      service.logout();

      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'refresh-xyz' });
      req.flush(null);

      expect(localStorage.getItem(ACCESS_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
      expect(service.isLoggedIn()).toBeFalse();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('Roles management (CLIENT, AGENCE, ADMIN)', () => {
    it('devrait identifier correctement les rôles depuis le user ou le token', () => {
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({
          ...mockUser,
          roles: ['ROLE_ADMIN', 'AGENCE'],
        }),
      );
      service.currentUser.set({
        ...mockUser,
        roles: ['ROLE_ADMIN', 'AGENCE'],
      });

      expect(service.getUserRoles()).toEqual(['ADMIN', 'AGENCE']);
      expect(service.hasRole('ADMIN')).toBeTrue();
      expect(service.hasRole('ROLE_ADMIN')).toBeTrue();
      expect(service.hasRole('AGENCE')).toBeTrue();
      expect(service.hasRole('CLIENT')).toBeFalse();
      expect(service.hasAnyRole(['CLIENT', 'AGENCE'])).toBeTrue();
    });

    it('devrait fallback sur les claims JWT si user non présent', () => {
      const adminToken = createFakeJwt({
        sub: 'admin@locavite.ma',
        roles: ['ADMIN'],
        exp: Math.floor(Date.now() / 1000) + 3600,
      });
      localStorage.setItem(ACCESS_KEY, adminToken);

      expect(service.hasRole('ADMIN')).toBeTrue();
      expect(service.hasRole('CLIENT')).toBeFalse();
    });
  });

  describe('Auto-refresh du token avant expiration', () => {
    it('devrait planifier un rafraîchissement automatique avant expiration', fakeAsync(() => {
      localStorage.setItem(REFRESH_KEY, 'valid-refresh-token');
      spyOn(service, 'refreshToken').and.callThrough();

      // expiresIn de 65 secondes -> refresh au bout de Math.max(65000 - 60000, 5000) = 5000 ms
      service.startAutoRefresh(65);

      tick(4999);
      expect(service.refreshToken).not.toHaveBeenCalled();

      tick(1);
      expect(service.refreshToken).toHaveBeenCalled();

      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({
        accessToken: validToken,
        refreshToken: 'new-refresh-token',
        expiresIn: 3600,
        user: mockUser,
      });

      service.stopAutoRefresh();
    }));
  });

  describe('JWT decoding helpers', () => {
    it('devrait décoder un JWT valide', () => {
      const token = createFakeJwt({ email: 'test@locavite.ma', custom: 123 });
      const decoded = decodeJwt(token);
      expect(decoded?.['email']).toBe('test@locavite.ma');
      expect(decoded?.['custom']).toBe(123);
    });

    it('devrait retourner null pour un format JWT invalide', () => {
      expect(decodeJwt('not.a.valid.jwt.token')).toBeNull();
      expect(decodeJwt('invalid')).toBeNull();
    });

    it('isTokenExpired devrait détecter un token expiré', () => {
      expect(isTokenExpired(expiredToken)).toBeTrue();
      expect(isTokenExpired(validToken)).toBeFalse();
    });
  });
});

