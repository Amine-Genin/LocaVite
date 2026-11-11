import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

export interface ReservationStats {
  totalCount: number;
  monthlyRevenue: number;
  occupancyRate: number;
  activeCount: number;
  pendingCount: number;
}

@Component({
  selector: 'app-stats-cards',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './stats-cards.component.html',
  styleUrl: './stats-cards.component.scss',
})
export class StatsCardsComponent {
  @Input() stats: ReservationStats = {
    totalCount: 0,
    monthlyRevenue: 0,
    occupancyRate: 0,
    activeCount: 0,
    pendingCount: 0,
  };
}
