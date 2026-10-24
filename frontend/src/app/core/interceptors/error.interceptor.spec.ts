import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { errorInterceptor, resetErrorInterceptorState } from './error.interceptor';
import { AuthService, AuthResponse } from '../auth/auth.service';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    resetErrorInterceptorState();

    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'getToken',
      'getRefreshToken',
      'refreshToken',
      'logout',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    resetErrorInterceptorState();
    httpMock.verify();
  });

  it('Test : 401 → refresh automatique', () => {
    authServiceSpy.getRefreshToken.and.returnValue('valid-refresh-token');
    const mockAuthResponse: AuthResponse = {
      accessToken: 'brand-new-access-token',
      refreshToken: 'brand-new-refresh-token',
      expiresIn: 3600,
      user: {
        id: 1,
        nom: 'Alami',
        prenom: 'Karim',
        email: 'karim@example.com',
        telephone: '+212600000000',
        roles: ['CLIENT'],
        createdAt: '2026-01-01',
      },
    };
    authServiceSpy.refreshToken.and.returnValue(of(mockAuthResponse));

    let responseData: unknown = null;
    http.get('/api/users/profile').subscribe({
      next: (res) => {
        responseData = res;
      },
    });

    // 1ère tentative échoue avec 401
    const firstReq = httpMock.expectOne('/api/users/profile');
    firstReq.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken).toHaveBeenCalled();

    // La requête est rejouée avec le nouveau token
    const retryReq = httpMock.expectOne('/api/users/profile');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer brand-new-access-token');
    retryReq.flush({ profile: 'success' });

    expect(responseData).toEqual({ profile: 'success' });
    expect(authServiceSpy.logout).not.toHaveBeenCalled();
  });

  it('devrait déconnecter l\'utilisateur si 401 et aucun refresh token n\'est disponible', () => {
    authServiceSpy.getRefreshToken.and.returnValue(null);

    let caughtError: unknown = null;
    http.get('/api/users/profile').subscribe({
      error: (err) => {
        caughtError = err;
      },
    });

    const req = httpMock.expectOne('/api/users/profile');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    expect(caughtError).toBeTruthy();
  });

  it('devrait déconnecter l\'utilisateur si le refresh token échoue avec une erreur', () => {
    authServiceSpy.getRefreshToken.and.returnValue('expired-refresh-token');
    authServiceSpy.refreshToken.and.returnValue(throwError(() => new Error('Refresh failed')));

    let caughtError: unknown = null;
    http.get('/api/users/profile').subscribe({
      error: (err) => {
        caughtError = err;
      },
    });

    const req = httpMock.expectOne('/api/users/profile');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken).toHaveBeenCalled();
    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(caughtError).toBeTruthy();
  });

  it('devrait déconnecter immédiatement en cas de 401 sur /api/auth/login', () => {
    let caughtError: unknown = null;
    http.post('/api/auth/login', { email: 'test@locavite.ma', password: 'bad' }).subscribe({
      error: (err) => {
        caughtError = err;
      },
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush('Bad credentials', { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    expect(caughtError).toBeTruthy();
  });

  it('ne devrait pas intercepter les erreurs non-401 (ex: 500)', () => {
    let caughtError: unknown = null;
    http.get('/api/some-endpoint').subscribe({
      error: (err) => {
        caughtError = err;
      },
    });

    const req = httpMock.expectOne('/api/some-endpoint');
    req.flush('Server error', { status: 500, statusText: 'Server Error' });

    expect(authServiceSpy.logout).not.toHaveBeenCalled();
    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    expect((caughtError as { status?: number }).status).toBe(500);
  });
});
