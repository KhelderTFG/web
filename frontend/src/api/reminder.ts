import client from './client';
import type { ReminderResponse, ReminderRequest } from '../types';

export const remindersApi = {
  getByPatient: (patientId: string) =>
    client.get<ReminderResponse[]>(`/reminders/patient/${patientId}`),

  create: (data: ReminderRequest) =>
    client.post<ReminderResponse>('/reminders', data),

  update: (id: string, data: ReminderRequest) =>
    client.put<ReminderResponse>(`/reminders/${id}`, data),

  delete: (id: string) =>
    client.delete(`/reminders/${id}`),
};