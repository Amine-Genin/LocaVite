import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

export type UserRole = 'CLIENT' | 'AGENCE' | 'ADMIN';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: state?.url ? { returnUrl: state.url } : undefined,
    });
  }

  const expectedRoles: string[] =
    route.data?.['roles'] ?? (route.data?.['role'] ? [route.data['role']] : []);

  if (expectedRoles.length === 0 || authService.hasAnyRole(expectedRoles)) {
    return true;
  }

  return router.createUrlTree(['/login']);
};

export const hasRole = (role: UserRole | string): CanActivateFn => {
  return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    const routeWithRole = {
      ...route,
      data: { ...route.data, roles: [role] },
    } as unknown as ActivatedRouteSnapshot;
    return roleGuard(routeWithRole, state);
  };
};

