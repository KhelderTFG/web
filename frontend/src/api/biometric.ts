import client from './client';
import type { BiometricHistoryResponse, Page } from '../types';

export const biometricApi = {
  getPatientHistory: (
    patientId: string,
    from: string,
    to: string,
    page = 0,
    size = 20
  ) =>
    client.get<Page<BiometricHistoryResponse>>(
      `/biometric-records/patient/${patientId}`,
      { params: { from, to, page, size } }
    ),

  getLatestByDevice: (deviceId: string) =>
    client.get<BiometricHistoryResponse>(
      `/biometric-records/device/${deviceId}/latest`
    ),

  getRecentByDevice: (deviceId: string) =>
    client.get<BiometricHistoryResponse[]>(
      `/biometric-records/device/${deviceId}/recent`
    ),
};