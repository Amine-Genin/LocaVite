import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'vehicles',
    loadComponent: () =>
      import('./features/vehicles/vehicle-list/vehicle-list.component').then(
        (m) => m.VehicleListComponent,
      ),
  },
  {
    path: 'vehicles/:id',
    loadComponent: () =>
      import('./features/vehicles/vehicle-detail/vehicle-detail.component').then(
        (m) => m.VehicleDetailComponent,
      ),
  },
  {
    path: 'reservation/:vehicleId',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] },
    loadComponent: () =>
      import(
        './features/reservations/reservation-form.component'
      ).then((m) => m.ReservationFormComponent),
  },
  {
    path: 'my-reservations',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] },
    loadComponent: () =>
      import(
        './features/reservations/my-reservations.component'
      ).then((m) => m.MyReservationsComponent),
  },
  {
    path: 'reservations/:id',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] },
    loadComponent: () =>
      import('./features/reservations/reservation-success.component').then(
        (m) => m.ReservationSuccessComponent,
      ),
  },
  {
    path: 'admin/reservations',
    canActivate: [roleGuard],
    data: { roles: ['AGENCE', 'ADMIN'] },
    loadComponent: () =>
      import(
        './features/reservations/admin-reservations/admin-reservations.component'
      ).then((m) => m.AdminReservationsComponent),
  },
  {
    path: 'admin/vehicles/new',
    canActivate: [roleGuard],
    data: { roles: ['AGENCE', 'ADMIN'] },
    loadComponent: () =>
      import('./features/vehicles/vehicle-form/vehicle-form.component').then(
        (m) => m.VehicleFormComponent,
      ),
  },
  {
    path: 'admin/vehicles',
    canActivate: [roleGuard],
    data: { roles: ['AGENCE', 'ADMIN'] },
    loadComponent: () =>
      import(
        './features/vehicles/admin-vehicle-list-placeholder/admin-vehicle-list-placeholder.component'
      ).then((m) => m.AdminVehicleListPlaceholderComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'dashboard',
    redirectTo: 'profile',
    pathMatch: 'full',
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'offline',
    loadComponent: () =>
      import('./features/offline/offline.component').then((m) => m.OfflineComponent),
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
