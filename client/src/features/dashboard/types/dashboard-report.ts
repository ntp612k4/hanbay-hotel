export interface DashboardReportParams {
  from: string;
  to: string;
  currency?: string;
}

export interface DashboardPeriod {
  from: string;
  to: string;
  currency: string;
}

export interface RevenueSummary {
  grossRevenue: number;
  refundedAmount: number;
  netRevenue: number;
}

export interface OccupancySummary {
  occupiedRoomNights: number;
  availableRoomNights: number;
  occupancyRate: number;
}

export interface BookingSummary {
  totalBookings: number;
  pendingBookings: number;
  confirmedBookings: number;
  checkedInBookings: number;
  checkedOutBookings: number;
  cancelledBookings: number;
  expiredBookings: number;
  totalBookingValue: number;
  averageBookingValue: number;
  cancellationRate: number;
}

export interface DailyRevenue extends RevenueSummary {
  date: string;
}

export interface DailyOccupancy extends OccupancySummary {
  date: string;
}

export interface DailyBookings {
  date: string;
  totalBookings: number;
}

export interface DashboardReport {
  maintenance?: {
    recordedExpense: number;
    cashAfterMaintenance: number;
    completedWithoutCost: number;
    trend: { date: string; amount: number }[];
  };
  period: DashboardPeriod;
  revenue: RevenueSummary;
  occupancy: OccupancySummary;
  bookings: BookingSummary;
  revenueTrend: DailyRevenue[];
  occupancyTrend: DailyOccupancy[];
  bookingTrend: DailyBookings[];
}
