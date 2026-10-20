import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const createDummyRoute = (): ActivatedRouteSnapshot => ({} as ActivatedRouteSnapshot);
  const createDummySnapshot = (url = '/profile'): RouterStateSnapshot =>
    ({ url } as RouterStateSnapshot);

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: Router,
          useValue: {
            createUrlTree: jasmine.createSpy('createUrlTree').and.callFake((commands, extras) => ({
              commands,
              extras,
            } as unknown as UrlTree)),
          },
        },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('Test : route protégée sans token → redirection login', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(createDummyRoute(), createDummySnapshot('/profile')),
    );

    expect(authServiceSpy.isAuthenticated).toHaveBeenCalled();
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/profile' },
    });
    expect(result).toEqual(
      jasmine.objectContaining({
        commands: ['/login'],
      }),
    );
  });

  it('devrait autoriser l\'accès quand l\'utilisateur est authentifié', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(createDummyRoute(), createDummySnapshot('/profile')),
    );

    expect(authServiceSpy.isAuthenticated).toHaveBeenCalled();
    expect(router.createUrlTree).not.toHaveBeenCalled();
    expect(result).toBeTrue();
  });
});

