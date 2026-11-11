import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PriceQuote } from '../models/reservation.model';

@Component({
  selector: 'app-price-summary',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (quote; as price) {
      <dl class="price-summary" data-cy="price-summary">
        <div>
          <dt>Location ({{ price.jours }} jours)</dt>
          <dd data-cy="price-subtotal">{{ formatPrice(price.sousTotal) }}</dd>
        </div>
        <div>
          <dt>Options</dt>
          <dd data-cy="price-options">{{ formatPrice(price.options) }}</dd>
        </div>
        <div>
          <dt>Remise</dt>
          <dd data-cy="price-discount">- {{ formatPrice(price.remise) }}</dd>
        </div>
        <div>
          <dt>TVA</dt>
          <dd data-cy="price-tva">{{ formatPrice(price.tva) }}</dd>
        </div>
        <div>
          <dt>Caution</dt>
          <dd data-cy="price-caution">{{ formatPrice(price.caution) }}</dd>
        </div>
        <div class="total">
          <dt>Total</dt>
          <dd data-cy="price-total">{{ formatPrice(price.total) }}</dd>
        </div>
      </dl>
    } @else {
      <p class="alert error" data-cy="price-unavailable">
        Le prix n’est pas disponible pour ces dates.
      </p>
    }
  `,
  styles: [
    `
      .price-summary {
        margin: 0;
      }
      .price-summary div {
        display: flex;
        justify-content: space-between;
        padding: 0.7rem 0;
        border-bottom: 1px solid #e2e8f0;
      }
      .price-summary dt,
      .price-summary dd {
        margin: 0;
      }
      .price-summary .total {
        font-size: 1.2rem;
        font-weight: 700;
        border-bottom: 0;
      }
      .alert {
        padding: 1rem;
        border-radius: 0.5rem;
        margin: 1rem 0;
      }
      .error {
        color: #b91c1c;
        background: #fee2e2;
      }
    `,
  ],
})
export class PriceSummaryComponent {
  @Input() quote: PriceQuote | null = null;

  formatPrice(value: number | undefined): string {
    return `${(value ?? 0).toFixed(2)} MAD`;
  }
}
