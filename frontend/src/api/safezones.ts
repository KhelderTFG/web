import client from './client';
import type { SafeZoneResponse, SafeZoneRequest } from '../types';

export const safeZonesApi = {
  getByPatient: (patientId: string) =>
    client.get<SafeZoneResponse[]>(`/safe-zones/patient/${patientId}`),

  create: (data: SafeZoneRequest) =>
    client.post<SafeZoneResponse>('/safe-zones', data),

  delete: (zoneId: string) =>
    client.delete(`/safe-zones/${zoneId}`),
};