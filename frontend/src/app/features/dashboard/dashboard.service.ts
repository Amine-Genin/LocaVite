import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, finalize, map, shareReplay } from 'rxjs';
import { DashboardData, DashboardRange } from './dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly inFlight = new Map<string, Observable<DashboardData>>();

  getDashboard(range: DashboardRange): Observable<DashboardData> {
    const key = `${range.from}:${range.to}`;
    const cached = this.inFlight.get(key);
    if (cached) return cached;

    const params = new HttpParams().set('from', range.from).set('to', range.to);
    const request = this.http
      .get<DashboardData>('/api/admin/dashboard', { params })
      .pipe(
        shareReplay({ bufferSize: 1, refCount: false }),
        finalize(() => this.inFlight.delete(key)),
      );
    this.inFlight.set(key, request);
    return request;
  }

  getKpis(range: DashboardRange): Observable<DashboardData['kpis']> {
    return this.getDashboard(range).pipe(map((dashboard) => dashboard.kpis));
  }

  getCharts(range: DashboardRange): Observable<DashboardData['charts']> {
    return this.getDashboard(range).pipe(map((dashboard) => dashboard.charts));
  }
}
