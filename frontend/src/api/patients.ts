import client from './client';
import type {
  PatientResponse,
  PatientDetailResponse,
  PatientRequest,
  MedicalHistoryResponse,
} from '../types';

export const patientsApi = {
  getAll: () =>
    client.get<PatientResponse[]>('/patients'),

  getById: (id: string) =>
    client.get<PatientDetailResponse>(`/patients/${id}`),

  create: (data: PatientRequest) =>
    client.post<PatientDetailResponse>('/patients', data),

  update: (id: string, data: PatientRequest) =>
    client.put<PatientDetailResponse>(`/patients/${id}`, data),

  updateMedicalHistory: (id: string, data: Partial<MedicalHistoryResponse>) =>
    client.put<MedicalHistoryResponse>(`/patients/${id}/medical-history`, data),

  delete: (id: string) =>
    client.delete(`/patients/${id}`),

  removeDevice: (patientId: string) =>
    client.delete(`/patients/${patientId}/device`),
};