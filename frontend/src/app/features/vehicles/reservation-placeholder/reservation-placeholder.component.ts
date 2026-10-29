import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-reservation-placeholder',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, TranslateModule],
  template: `
    <div class="reservation-page" data-cy="reservation-page">
      <div class="container">
        <h1>Réservation du véhicule #{{ vehicleId }}</h1>
        <p>Le module de réservation pour ce véhicule est en cours de préparation.</p>
        <a
          mat-flat-button
          color="primary"
          [routerLink]="['/vehicles', vehicleId]"
          data-cy="back-to-vehicle-btn"
        >
          Retour à la fiche véhicule
        </a>
      </div>
    </div>
  `,
  styles: [
    `
      .reservation-page {
        min-height: 70vh;
        display: flex;
        align-items: center;
        justify-content: center;
        text-align: center;
        padding: 2rem;
      }
      .container {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 1rem;
        padding: 2.5rem;
        max-width: 520px;
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
      }
      h1 {
        font-size: 1.5rem;
        font-weight: 700;
        color: #0f172a;
        margin-bottom: 0.75rem;
      }
      p {
        color: #64748b;
        margin-bottom: 1.5rem;
      }
    `,
  ],
})
export class ReservationPlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly vehicleId = this.route.snapshot.paramMap.get('id');
}
