import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../auth/auth.service';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('Test : requête HTTP → header Authorization présent', () => {
    const fakeToken = 'mock-jwt-token-xyz';
    authServiceSpy.getToken.and.returnValue(fakeToken);

    http.get('/api/users/me').subscribe();

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${fakeToken}`);
    req.flush({ id: 1 });
  });

  it('ne devrait pas ajouter de header Authorization si aucun token n\'est présent', () => {
    authServiceSpy.getToken.and.returnValue(null);

    http.get('/api/public/data').subscribe();

    const req = httpMock.expectOne('/api/public/data');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});

