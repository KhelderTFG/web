import client from './client';
import type { AlertResponse, AlertStatus, Page } from '../types';

export const alertsApi = {
  getMyAlerts: (status: AlertStatus = 'ACTIVE', page = 0, size = 20) =>
    client.get<Page<AlertResponse>>('/alerts', {
      params: { status, page, size }
    }),

  getByPatient: (patientId: string, status?: AlertStatus) =>
    client.get<Page<AlertResponse>>(`/alerts/patient/${patientId}`, {
      params: { status }
    }),

  resolve: (alertId: string, observations?: string) =>
    client.put<AlertResponse>(`/alerts/${alertId}/resolve`, { observations }),

  cancel: (alertId: string) =>
    client.put<AlertResponse>(`/alerts/${alertId}/cancel`),
};