import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

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
    path: 'reservation/:id',
    loadComponent: () =>
      import(
        './features/vehicles/reservation-placeholder/reservation-placeholder.component'
      ).then((m) => m.ReservationPlaceholderComponent),
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
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
