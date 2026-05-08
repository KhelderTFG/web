import client from './client';
import type { SmartwatchResponse, PairingApproveRequest } from '../types';

export const smartwatchesApi = {
  getMyDevices: () =>
    client.get<SmartwatchResponse[]>('/smartwatches'),

  getStatus: (deviceId: string) =>
    client.get<SmartwatchResponse>(`/smartwatches/${deviceId}/status`),

  approvePairing: (request: PairingApproveRequest) =>
    client.post<void>('/pairing/approve', request),
};