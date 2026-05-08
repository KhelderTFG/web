import client from './client';

export const reportsApi = {
  generate: (
    patientId: string,
    from:      string,
    to:        string,
    format:    'pdf' | 'csv'
  ) =>
    client.get(`/reports/${patientId}`, {
      params:       { from, to, format },
      responseType: 'blob',
    }),
};