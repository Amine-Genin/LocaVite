import { AfterViewInit, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Chart, registerables } from 'chart.js';
import { catchError, forkJoin, of } from 'rxjs';
import { DashboardService } from './dashboard.service';

Chart.register(...registerables);
type Period = '7j' | '30j' | '90j' | '1an' | 'custom';
interface Row { id?: number; code?: string; nom?: string; clientNom?: string; userId?: number; vehicleId?: number; categorie?: string; montant?: number; prixTotal?: number; statut?: string; createdAt?: string; dateDebut?: string; marque?: string; modele?: string; titre?: string; }

@Component({
  selector: 'app-admin-dashboard', standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './admin-dashboard.component.html', styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly service = inject(DashboardService);
  private charts: Chart[] = [];
  readonly loading = signal(true);
  readonly error = signal('');
  readonly period = signal<Period>('30j');
  readonly customRange = signal<{ from: string; to: string } | null>(null);
  readonly reservations = signal<Row[]>([]);
  readonly payments = signal<Row[]>([]);
  readonly contacts = signal<Row[]>([]);
  readonly kpis = signal({ revenue: 0, revenueChange: 0, active: 0, available: 0, totalVehicles: 0, occupancy: 0, clients: 0, sales: 0 });

  ngOnInit(): void { this.load(); }
  ngAfterViewInit(): void { setTimeout(() => this.drawCharts(), 0); }
  ngOnDestroy(): void { this.charts.forEach((chart) => chart.destroy()); }

  setPeriod(value: Period, from?: string, to?: string): void {
    this.period.set(value);
    this.customRange.set(value === 'custom' && from && to ? { from, to } : null);
    this.load();
  }
  printPdf(): void { window.print(); }

  load(): void {
    this.loading.set(true); this.error.set('');
    forkJoin({ data: this.service.getKpis(), contacts: this.service.getCharts().pipe(catchError(() => of([]))) }).subscribe({
      next: ({ data, contacts }) => {
        const reservations = data.reservations?.content ?? [];
        const payments = data.payments?.content ?? [];
        const vehicles = data.vehicles?.content ?? [];
        const sales = data.sales?.content ?? [];
        const periodDays = this.period() === '7j' ? 7 : this.period() === '90j' ? 90 : this.period() === '1an' ? 365 : 30;
        const range = this.customRange();
        const after = range ? new Date(`${range.from}T00:00:00`) : new Date();
        if (!range) after.setDate(after.getDate() - periodDays);
        const through = range ? new Date(`${range.to}T23:59:59`) : new Date();
        const paid = payments.filter((p: Row) => p.statut === 'SUCCESS' && p.createdAt && new Date(p.createdAt) >= after && new Date(p.createdAt) <= through);
        const currentRevenue = paid.reduce((sum: number, p: Row) => sum + (Number(p.montant) || 0), 0);
        const previousStart = new Date(after); previousStart.setDate(previousStart.getDate() - (range ? Math.max(1, Math.ceil((through.getTime() - after.getTime()) / 86400000)) : periodDays));
        const previous = payments.filter((p: Row) => p.statut === 'SUCCESS' && p.createdAt && new Date(p.createdAt) >= previousStart && new Date(p.createdAt) < after).reduce((sum: number, p: Row) => sum + (Number(p.montant) || 0), 0);
        const active = reservations.filter((r: Row) => ['PENDING', 'CONFIRMED', 'ACTIVE'].includes(r.statut ?? '')).length;
        const available = vehicles.filter((v: Row) => v.statut === 'PUBLISHED').length;
        const uniqueClients = new Set(reservations.map((r: any) => r.userId).filter(Boolean)).size;
        this.reservations.set(reservations.slice(0, 5)); this.payments.set(payments.slice(0, 5));
        const contactItems = Array.isArray(contacts) ? contacts : ((contacts as any)?.content ?? []);
        this.contacts.set(contactItems.slice(0, 5));
        this.kpis.set({ revenue: currentRevenue, revenueChange: previous ? ((currentRevenue - previous) / previous) * 100 : 0, active, available, totalVehicles: data.vehicles?.totalElements ?? vehicles.length, occupancy: vehicles.length ? Math.round((vehicles.filter((v: Row) => v.statut === 'RENTED').length / vehicles.length) * 100) : 0, clients: uniqueClients, sales: sales.filter((s: Row) => ['DRAFT', 'PUBLISHED'].includes(s.statut ?? '')).length });
        this.loading.set(false); setTimeout(() => this.drawCharts(data), 0);
      },
      error: () => { this.error.set('Impossible de charger les indicateurs.'); this.loading.set(false); },
    });
  }

  private drawCharts(data?: any): void {
    this.charts.forEach((chart) => chart.destroy()); this.charts = [];
    if (!data) return;
    const reservations: Row[] = data.reservations?.content ?? [];
    const payments: Row[] = data.payments?.content ?? [];
    const vehicles: Row[] = data.vehicles?.content ?? [];
    const create = (id: string, type: any, labels: string[], datasets: any[], options: any = {}) => {
      const canvas = document.getElementById(id) as HTMLCanvasElement | null;
      if (canvas) this.charts.push(new Chart(canvas, { type, data: { labels, datasets }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: type === 'doughnut' || type === 'pie', position: 'bottom' as const } }, ...options } }));
    };
    const months = Array.from({ length: 12 }, (_, i) => { const date = new Date(); date.setMonth(date.getMonth() - 11 + i); return date; });
    const revenues = months.map((m) => payments.filter((p) => p.statut === 'SUCCESS' && p.createdAt && new Date(p.createdAt).getMonth() === m.getMonth() && new Date(p.createdAt).getFullYear() === m.getFullYear()).reduce((s, p) => s + (Number(p.montant) || 0), 0));
    create('revenue-chart', 'line', months.map((m) => m.toLocaleDateString('fr-FR', { month: 'short' })), [{ label: 'Revenus (MAD)', data: revenues, borderColor: '#16a36a', backgroundColor: 'rgba(22,163,106,.12)', fill: true, tension: .35 }]);
    const statuses = ['PENDING', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED'];
    create('status-chart', 'doughnut', statuses, [{ data: statuses.map((s) => reservations.filter((r) => r.statut === s).length), backgroundColor: ['#f5a623', '#4386f5', '#16a36a', '#8a94a6', '#ed6a5e'], borderWidth: 0 }]);
    const categories = [...new Set(vehicles.map((v) => v.categorie || 'Autre'))];
    create('category-chart', 'pie', categories, [{ data: categories.map((c) => vehicles.filter((v) => (v.categorie || 'Autre') === c).length), backgroundColor: ['#16a36a', '#4386f5', '#f5a623', '#9675ce', '#ed6a5e'], borderWidth: 0 }]);
    const top = vehicles.map((v) => ({ name: `${v.marque ?? ''} ${v.modele ?? ''}`.trim(), count: reservations.filter((r: any) => r.vehicleId === v.id).length })).sort((a, b) => b.count - a.count).slice(0, 5);
    create('top-chart', 'bar', top.map((v) => v.name), [{ label: 'Réservations', data: top.map((v) => v.count), backgroundColor: '#16a36a', borderRadius: 6 }]);
    create('occupancy-chart', 'bar', months.slice(-6).map((m) => m.toLocaleDateString('fr-FR', { month: 'short' })), [{ label: 'Occupation (%)', data: months.slice(-6).map(() => this.kpis().occupancy), backgroundColor: '#4386f5', borderRadius: 6 }], { scales: { y: { beginAtZero: true, max: 100 } } });
  }
}
