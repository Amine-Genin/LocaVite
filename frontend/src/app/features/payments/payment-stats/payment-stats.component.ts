import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

export interface PaymentDashboardStats {
  monthlyRevenue: number;
  transactionCount: number;
  successRate: number;
  totalRefunded: number;
  methodCounts: Record<string, number>;
  dailyRevenue: { date: string; amount: number }[];
}

@Component({
  selector: 'app-payment-stats',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  template: `
    <div class="stats-wrapper" data-testid="payment-stats">
      <!-- 4 KPI Cards -->
      <div class="kpi-grid">
        <mat-card class="kpi-card" data-testid="stat-revenue">
          <div class="kpi-header">
            <span class="kpi-title">Revenus du mois</span>
            <div class="icon-circle icon-green">
              <mat-icon>trending_up</mat-icon>
            </div>
          </div>
          <div class="kpi-body">
            <span class="kpi-value">{{ stats.monthlyRevenue | number: '1.0-0' }} MAD</span>
            <span class="kpi-trend text-green">+ Ce mois-ci</span>
          </div>
        </mat-card>

        <mat-card class="kpi-card" data-testid="stat-count">
          <div class="kpi-header">
            <span class="kpi-title">Transactions</span>
            <div class="icon-circle icon-blue">
              <mat-icon>receipt</mat-icon>
            </div>
          </div>
          <div class="kpi-body">
            <span class="kpi-value">{{ stats.transactionCount }}</span>
            <span class="kpi-sub">Total enregistré</span>
          </div>
        </mat-card>

        <mat-card class="kpi-card" data-testid="stat-success-rate">
          <div class="kpi-header">
            <span class="kpi-title">Taux de succès</span>
            <div class="icon-circle icon-teal">
              <mat-icon>check_circle</mat-icon>
            </div>
          </div>
          <div class="kpi-body">
            <span class="kpi-value">{{ stats.successRate }}%</span>
            <span class="kpi-sub">Paiements validés</span>
          </div>
        </mat-card>

        <mat-card class="kpi-card" data-testid="stat-refunded">
          <div class="kpi-header">
            <span class="kpi-title">Total Remboursé</span>
            <div class="icon-circle icon-orange">
              <mat-icon>replay</mat-icon>
            </div>
          </div>
          <div class="kpi-body">
            <span class="kpi-value">{{ stats.totalRefunded | number: '1.0-0' }} MAD</span>
            <span class="kpi-sub">Avoirs & remboursements</span>
          </div>
        </mat-card>
      </div>

      <!-- 2 Charts: Évolution Revenus (Line) & Répartition Méthodes (Pie) -->
      <div class="charts-grid">
        <mat-card class="chart-card" data-testid="chart-revenue-trend">
          <div class="chart-header">
            <mat-icon class="chart-icon">show_chart</mat-icon>
            <h3>Évolution des Revenus</h3>
          </div>
          <div class="chart-container">
            <canvas #revenueChartCanvas></canvas>
          </div>
        </mat-card>

        <mat-card class="chart-card" data-testid="chart-method-pie">
          <div class="chart-header">
            <mat-icon class="chart-icon">pie_chart</mat-icon>
            <h3>Répartition par Méthode</h3>
          </div>
          <div class="chart-container pie-container">
            <canvas #methodChartCanvas></canvas>
          </div>
        </mat-card>
      </div>
    </div>
  `,
  styles: [
    `
      .stats-wrapper {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
        margin-bottom: 2rem;
      }
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
        gap: 1.25rem;
      }
      .kpi-card {
        padding: 1.25rem;
        border-radius: 1rem;
        background: #ffffff;
        box-shadow: 0 4px 15px -3px rgba(15, 23, 42, 0.06);
        border: 1px solid #f1f5f9;
      }
      .kpi-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }
      .kpi-title {
        font-size: 0.85rem;
        font-weight: 600;
        color: #64748b;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .icon-circle {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .icon-circle mat-icon {
        font-size: 22px;
        width: 22px;
        height: 22px;
      }
      .icon-green {
        background: #dcfce7;
        color: #15803d;
      }
      .icon-blue {
        background: #e0f2fe;
        color: #0369a1;
      }
      .icon-teal {
        background: #ccfbf1;
        color: #0f766e;
      }
      .icon-orange {
        background: #ffedd5;
        color: #c2410c;
      }
      .kpi-body {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .kpi-value {
        font-size: 1.6rem;
        font-weight: 700;
        color: #0f172a;
      }
      .kpi-sub {
        font-size: 0.8rem;
        color: #94a3b8;
      }
      .text-green {
        color: #16a34a;
        font-size: 0.8rem;
        font-weight: 600;
      }
      .charts-grid {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 1.25rem;
      }
      @media (max-width: 960px) {
        .charts-grid {
          grid-template-columns: 1fr;
        }
      }
      .chart-card {
        padding: 1.25rem;
        border-radius: 1rem;
        background: #ffffff;
        box-shadow: 0 4px 15px -3px rgba(15, 23, 42, 0.06);
        border: 1px solid #f1f5f9;
      }
      .chart-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 1rem;
      }
      .chart-header h3 {
        margin: 0;
        font-size: 1.05rem;
        font-weight: 600;
        color: #1e293b;
      }
      .chart-icon {
        color: #b4532a;
      }
      .chart-container {
        position: relative;
        height: 260px;
        width: 100%;
      }
      .pie-container {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    `,
  ],
})
export class PaymentStatsComponent implements AfterViewInit, OnChanges {
  @Input() stats: PaymentDashboardStats = {
    monthlyRevenue: 0,
    transactionCount: 0,
    successRate: 0,
    totalRefunded: 0,
    methodCounts: {},
    dailyRevenue: [],
  };

  @ViewChild('revenueChartCanvas') revenueChartRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('methodChartCanvas') methodChartRef?: ElementRef<HTMLCanvasElement>;

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private revenueChart?: Chart<any>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private methodChart?: Chart<any>;

  ngAfterViewInit(): void {
    this.renderCharts();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['stats'] && (this.revenueChart || this.methodChart)) {
      this.renderCharts();
    }
  }

  private renderCharts(): void {
    if (!this.revenueChartRef || !this.methodChartRef) return;

    // 1. Line Chart : Évolution des revenus
    if (this.revenueChart) {
      this.revenueChart.destroy();
    }
    const revCtx = this.revenueChartRef.nativeElement.getContext('2d');
    if (revCtx) {
      const labels = this.stats.dailyRevenue.map((d) => d.date);
      const data = this.stats.dailyRevenue.map((d) => d.amount);

      this.revenueChart = new Chart(revCtx, {
        type: 'line',
        data: {
          labels: labels.length ? labels : ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'],
          datasets: [
            {
              label: 'Revenus (MAD)',
              data: data.length ? data : [0, 0, 0, 0],
              borderColor: '#b4532a',
              backgroundColor: 'rgba(180, 83, 42, 0.08)',
              borderWidth: 2.5,
              tension: 0.35,
              fill: true,
              pointBackgroundColor: '#b4532a',
              pointRadius: 4,
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { color: '#64748b' },
              grid: { color: '#f1f5f9' },
            },
            x: {
              ticks: { color: '#64748b' },
              grid: { display: false },
            },
          },
        },
      });
    }

    // 2. Pie Chart : Répartition par méthode
    if (this.methodChart) {
      this.methodChart.destroy();
    }
    const methodCtx = this.methodChartRef.nativeElement.getContext('2d');
    if (methodCtx) {
      const cmi = this.stats.methodCounts['CMI'] || 0;
      const cashDelivery = this.stats.methodCounts['CASH_DELIVERY'] || 0;
      const cashAgency = this.stats.methodCounts['CASH_AGENCY'] || 0;
      const transfer = this.stats.methodCounts['BANK_TRANSFER'] || 0;

      const hasData = cmi + cashDelivery + cashAgency + transfer > 0;

      this.methodChart = new Chart(methodCtx, {
        type: 'doughnut',
        data: {
          labels: ['CMI (Carte)', 'Espèces Livraison', 'Espèces Agence', 'Virement'],
          datasets: [
            {
              data: hasData ? [cmi, cashDelivery, cashAgency, transfer] : [1, 0, 0, 0],
              backgroundColor: ['#2563eb', '#16a34a', '#f59e0b', '#64748b'],
              borderWidth: 2,
              borderColor: '#ffffff',
            },
          ],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: {
                boxWidth: 12,
                color: '#334155',
                font: { size: 11 },
              },
            },
          },
          cutout: '65%',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any,
      });
    }
  }
}
