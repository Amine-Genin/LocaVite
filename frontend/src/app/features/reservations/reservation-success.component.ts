import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-reservation-success',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule],
  template: `
    <main class="success-page">
      <section class="success-card">
        <div class="success-icon" aria-hidden="true">✓</div>
        <h1>Réservation confirmée</h1>
        <p>
          Votre réservation <strong>{{ reservationId }}</strong> a bien été enregistrée.
          Un e-mail de confirmation vous sera envoyé.
        </p>
        <a mat-flat-button color="primary" routerLink="/vehicles">Retour au catalogue</a>
      </section>
    </main>
  `,
  styles: [`
    .success-page { min-height: 70vh; display: grid; place-items: center; padding: 2rem; }
    .success-card { max-width: 520px; padding: 2.5rem; text-align: center; border-radius: 1rem;
      background: #fff; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, .12); }
    .success-icon { width: 3.5rem; height: 3.5rem; margin: 0 auto 1rem; border-radius: 50%;
      display: grid; place-items: center; color: #fff; background: #16a34a; font-size: 2rem; }
  `],
})
export class ReservationSuccessComponent {
  private readonly route = inject(ActivatedRoute);
  readonly reservationId = this.route.snapshot.paramMap.get('id');
}
