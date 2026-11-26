export type DashboardPeriod = '7d' | '30d' | '90d' | '1y' | 'custom';

export interface DashboardRange {
  from: string;
  to: string;
}

export interface DashboardKpis {
  monthlyRevenue: number;
  previousMonthRevenue: number;
  activeReservations: number;
  availableVehicles: number;
  totalVehicles: number;
  occupancyRate: number;
  clients: number;
  salesInProgress: number;
}

export interface DashboardCountPoint {
  label: string;
  count: number;
}

export interface DashboardData {
  kpis: DashboardKpis;
  charts: {
    revenue: { label: string; amount: number }[];
    reservationsByStatus: DashboardCountPoint[];
    vehiclesByCategory: DashboardCountPoint[];
    topVehicles: DashboardCountPoint[];
    occupancyByMonth: { label: string; rate: number }[];
  };
  recent: {
    reservations: {
      id: number;
      code: string;
      client: string;
      vehicle: string;
      status: string;
      startDate: string;
      endDate: string;
      amount: number;
      createdAt: string;
    }[];
    payments: {
      id: number;
      code: string;
      client: string;
      amount: number;
      status: string;
      createdAt: string;
    }[];
    contacts: {
      id: number;
      saleListingId: number;
      name: string;
      email: string;
      message: string;
      status: string;
      createdAt: string;
    }[];
  };
}
