import client from './client';

export const gpsApi = {
  getLatestByDevice: (deviceId: string) =>
    client.get<{ latitude: number; longitude: number }>(
      `/gps/device/${deviceId}/latest`
    ),
};