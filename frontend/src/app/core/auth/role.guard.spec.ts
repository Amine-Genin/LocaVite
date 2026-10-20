import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { roleGuard, hasRole } from './role.guard';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const createRouteWithData = (data: Record<string, unknown>): ActivatedRouteSnapshot =>
    ({ data } as unknown as ActivatedRouteSnapshot);

  const createDummySnapshot = (url = '/admin'): RouterStateSnapshot =>
    ({ url } as RouterStateSnapshot);

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'isAuthenticated',
      'hasRole',
      'hasAnyRole',
    ]);

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

  it('devrait rediriger vers /login si non authentifié', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);

    const route = createRouteWithData({ roles: ['ADMIN'] });
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, createDummySnapshot('/admin')),
    );

    expect(authServiceSpy.isAuthenticated).toHaveBeenCalled();
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin' },
    });
    expect(result).toEqual(jasmine.objectContaining({ commands: ['/login'] }));
  });

  it('devrait autoriser l\'accès si le rôle requis (ADMIN) est présent', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('ADMIN'));

    const route = createRouteWithData({ roles: ['ADMIN'] });
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, createDummySnapshot('/admin')),
    );

    expect(authServiceSpy.isAuthenticated).toHaveBeenCalled();
    expect(authServiceSpy.hasAnyRole).toHaveBeenCalledWith(['ADMIN']);
    expect(result).toBeTrue();
  });

  it('devrait autoriser l\'accès si le rôle CLIENT ou AGENCE est requis et présent', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.callFake((roles: string[]) => roles.includes('AGENCE'));

    const route = createRouteWithData({ roles: ['CLIENT', 'AGENCE'] });
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, createDummySnapshot('/agency-panel')),
    );

    expect(result).toBeTrue();
  });

  it('devrait rediriger si l\'utilisateur authentifié n\'a pas le rôle requis', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(false);

    const route = createRouteWithData({ roles: ['ADMIN'] });
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, createDummySnapshot('/admin')),
    );

    expect(result).toEqual(jasmine.objectContaining({ commands: ['/login'] }));
  });

  it('devrait fonctionner avec un rôle unique passé dans data.role', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(true);

    const route = createRouteWithData({ role: 'CLIENT' });
    const result = TestBed.runInInjectionContext(() =>
      roleGuard(route, createDummySnapshot('/client-space')),
    );

    expect(authServiceSpy.hasAnyRole).toHaveBeenCalledWith(['CLIENT']);
    expect(result).toBeTrue();
  });

  it('devrait supporter le helper hasRole()', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.hasAnyRole.and.returnValue(true);

    const guardFn = hasRole('ADMIN');
    const route = createRouteWithData({});
    const result = TestBed.runInInjectionContext(() =>
      guardFn(route, createDummySnapshot('/admin')),
    );

    expect(result).toBeTrue();
  });
});

