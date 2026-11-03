import { useQuery } from '@tanstack/react-query';

import type { DashboardReportParams } from '../types/dashboard-report';
import { getDashboardReport } from './dashboard-api';

export const dashboardKeys = {
  all: ['manager-dashboard'] as const,
  report: (params: DashboardReportParams) => [...dashboardKeys.all, 'report', params] as const,
};

export const useDashboardReport = (params: DashboardReportParams) =>
  useQuery({
    queryKey: dashboardKeys.report(params),
    queryFn: () => getDashboardReport(params),
  });
