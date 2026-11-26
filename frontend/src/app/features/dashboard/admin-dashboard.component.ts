import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { EMPTY, Subject, catchError, switchMap, tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DashboardData, DashboardPeriod, DashboardRange } from './dashboard.models';
import { DashboardService } from './dashboard.service';

Chart.register(...registerables);

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly rangeRequests = new Subject<DashboardRange>();
  private readonly chartInstances: { destroy: () => void }[] = [];
  private viewReady = false;

  readonly selectedPeriod = signal<DashboardPeriod>('1y');
  readonly customFrom = signal('');
  readonly customTo = signal('');
  readonly dashboard = signal<DashboardData | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  @ViewChild('revenueCanvas') private revenueCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusCanvas') private statusCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryCanvas') private categoryCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('topVehiclesCanvas') private topVehiclesCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('occupancyCanvas') private occupancyCanvas?: ElementRef<HTMLCanvasElement>;

  ngOnInit(): void {
    this.rangeRequests
      .pipe(
        tap(() => {
          this.isLoading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap((range) =>
          this.dashboardService.getDashboard(range).pipe(
            catchError(() => {
              this.errorMessage.set('Impossible de charger les indicateurs. Réessayez dans un instant.');
              this.isLoading.set(false);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => {
        this.dashboard.set(data);
        this.isLoading.set(false);
        if (this.viewReady) this.renderCharts(data);
      });

    this.selectPeriod('1y');
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    const data = this.dashboard();
    if (data) this.renderCharts(data);
  }

  ngOnDestroy(): void {
    this.chartInstances.splice(0).forEach((chart) => chart.destroy());
  }

  selectPeriod(period: DashboardPeriod): void {
    this.selectedPeriod.set(period);
    if (period === 'custom') return;

    const end = this.todayUtc();
    const days: Record<Exclude<DashboardPeriod, 'custom' | '1y'>, number> = {
      '7d': 7,
      '30d': 30,
      '90d': 90,
    };
    const range = {
      from: period === '1y' ? this.startOfTwelveMonthWindow(end) : this.addUtcDays(end, -(days[period] - 1)),
      to: end,
    };
    this.customFrom.set(range.from);
    this.customTo.set(range.to);
    this.loadRange(range);
  }

  applyCustomPeriod(): void {
    const from = this.customFrom();
    const to = this.customTo();
    if (!from || !to || from > to) {
      this.errorMessage.set('Choisissez une période valide.');
      return;
    }
    this.selectedPeriod.set('custom');
    this.loadRange({ from, to });
  }

  revenueChange(): number {
    const kpis = this.dashboard()?.kpis;
    if (!kpis || kpis.previousMonthRevenue === 0) return kpis?.monthlyRevenue ? 100 : 0;
    return ((kpis.monthlyRevenue - kpis.previousMonthRevenue) / kpis.previousMonthRevenue) * 100;
  }

  exportPdf(): void {
    window.print();
  }

  private loadRange(range: DashboardRange): void {
    this.rangeRequests.next(range);
  }

  private renderCharts(data: DashboardData): void {
    this.chartInstances.splice(0).forEach((chart) => chart.destroy());
    const grid = '#e8edf3';
    const text = '#64748b';
    const palette = ['#2563eb', '#14b8a6', '#f59e0b', '#8b5cf6', '#f43f5e', '#64748b'];

    this.createChart(this.revenueCanvas, {
      type: 'line',
      data: {
        labels: data.charts.revenue.map((point) => point.label),
        datasets: [{
          label: 'Revenus (MAD)',
          data: data.charts.revenue.map((point) => point.amount),
          borderColor: '#2563eb',
          backgroundColor: 'rgba(37, 99, 235, .12)',
          fill: true,
          tension: 0.35,
          pointRadius: 3,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: text, maxTicksLimit: 8 } },
          y: { beginAtZero: true, grid: { color: grid }, ticks: { color: text } },
        },
      },
    });

    this.createChart(this.statusCanvas, {
      type: 'doughnut',
      data: {
        labels: data.charts.reservationsByStatus.map((point) => this.statusLabel(point.label)),
        datasets: [{
          data: data.charts.reservationsByStatus.map((point) => point.count),
          backgroundColor: palette,
          borderWidth: 0,
        }],
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '66%', plugins: { legend: { position: 'bottom', labels: { color: text, usePointStyle: true, padding: 16 } } } },
    } as ChartConfiguration<'doughnut'>);

    this.createChart(this.categoryCanvas, {
      type: 'pie',
      data: {
        labels: data.charts.vehiclesByCategory.map((point) => point.label),
        datasets: [{ data: data.charts.vehiclesByCategory.map((point) => point.count), backgroundColor: palette, borderWidth: 0 }],
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { color: text, usePointStyle: true, padding: 16 } } } },
    });

    this.createChart(this.topVehiclesCanvas, {
      type: 'bar',
      data: {
        labels: data.charts.topVehicles.map((point) => point.label),
        datasets: [{ label: 'Réservations', data: data.charts.topVehicles.map((point) => point.count), backgroundColor: '#14b8a6', borderRadius: 6 }],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { beginAtZero: true, grid: { color: grid }, ticks: { color: text, precision: 0 } }, y: { grid: { display: false }, ticks: { color: text } } },
      },
    });

    this.createChart(this.occupancyCanvas, {
      type: 'bar',
      data: {
        labels: data.charts.occupancyByMonth.map((point) => point.label),
        datasets: [{ label: 'Occupation (%)', data: data.charts.occupancyByMonth.map((point) => point.rate), backgroundColor: '#8b5cf6', borderRadius: 6 }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { x: { grid: { display: false }, ticks: { color: text } }, y: { beginAtZero: true, max: 100, grid: { color: grid }, ticks: { color: text, callback: (value) => `${value}%` } } },
      },
    });
  }

  private createChart<TType extends ChartType>(
    canvas: ElementRef<HTMLCanvasElement> | undefined,
    config: ChartConfiguration<TType>,
  ): void {
    if (canvas) this.chartInstances.push(new Chart<TType>(canvas.nativeElement, config));
  }

  private statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente', CONFIRMED: 'Confirmées', ACTIVE: 'En cours',
      COMPLETED: 'Terminées', CANCELLED: 'Annulées',
    };
    return labels[status] ?? status;
  }

  private todayUtc(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private addUtcDays(dateValue: string, days: number): string {
    const date = new Date(`${dateValue}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() + days);
    return date.toISOString().slice(0, 10);
  }

  private startOfTwelveMonthWindow(dateValue: string): string {
    const date = new Date(`${dateValue}T00:00:00Z`);
    date.setUTCDate(1);
    date.setUTCMonth(date.getUTCMonth() - 11);
    return date.toISOString().slice(0, 10);
  }
}
