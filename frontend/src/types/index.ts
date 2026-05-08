// ---- Auth ----
export interface AuthResponse {
  token:       string;
  caregiverId: string;
  name:        string;
  email:       string;
}

export interface LoginRequest {
  email:    string;
  password: string;
}

export interface RegisterRequest {
  name:     string;
  email:    string;
  password: string;
  phone?:   string;
}

// ---- Pacientes ----
export interface PatientResponse {
  patientId:        string;
  fullName:         string;
  dateOfBirth:      string;
  age:              number;
  activeDeviceId:   string | null;
  deviceConnected:  boolean;
  batteryLevel:     number | null;
  activeAlertsCount: number;
}

export interface PatientDetailResponse extends PatientResponse {
  medicalHistory: MedicalHistoryResponse | null;
}

export interface MedicalHistoryResponse {
  historyId:             string;
  bloodType:             string | null;
  allergies:             string | null;
  chronicConditions:     string | null;
  emergencyInstructions: string | null;
}

export interface PatientRequest {
  fullName:    string;
  dateOfBirth: string;
}

// ---- Alertas ----
export type AlertType =
  | 'SOS_MANUAL'
  | 'FALL_DETECTED'
  | 'HEART_RATE_HIGH'
  | 'HEART_RATE_LOW'
  | 'BATTERY_LOW'
  | 'GEOFENCE_EXIT'
  | 'SPO2_LOW';

export type AlertStatus = 'ACTIVE' | 'RESOLVED' | 'CANCELLED';

export interface AlertResponse {
  alertId:     string;
  deviceId:    string;
  patientId:   string | null;
  patientName: string | null;
  alertType:   AlertType;
  status:      AlertStatus;
  latitude:    number | null;
  longitude:   number | null;
  heartRate:   number | null;
  batteryLevel: number | null;
  timestamp:   string;
}

// ---- Recordatorios ----
export type ReminderStatus = 'PENDING' | 'SENT_TO_WATCH' | 'CONFIRMED' | 'MISSED';

export interface ReminderResponse {
  reminderId:   string;
  patientId:    string;
  patientName:  string;
  caregiverId:  string;
  message:      string;
  scheduledDate: string;
  status:       ReminderStatus;
}

export interface ReminderRequest {
  patientId:    string;
  message:      string;
  scheduledDate: string;
}

// ---- Registros biométricos ----
export interface BiometricHistoryResponse {
  recordId:    string;
  deviceId:    string;
  heartRate:   number | null;
  spO2:        number | null;
  steps:       number | null;
  temperature: number | null;
  timestamp:   string;
}

// ---- Smartwatch ----
export interface SmartwatchResponse {
  deviceId:            string;
  patientId:           string;
  patientName:         string;
  batteryLevel:        number | null;
  connectionStatus:    boolean;
  lastPing:            string | null;
  minutesSinceLastPing: number | null;
}

// ---- Zonas seguras ----
export interface SafeZoneResponse {
  zoneId:       string;
  patientId:    string;
  patientName:  string;
  caregiverId:  string;
  latitude:     number;
  longitude:    number;
  radiusMeters: number;
}

export interface SafeZoneRequest {
  patientId:    string;
  latitude:     number;
  longitude:    number;
  radiusMeters: number;
}

// ---- Paginación ----
export interface Page<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  size:             number;
  number:           number;
}

// ---- WebSocket ----
export type AlertNotification = AlertResponse;

// ---- Pairing ----
export interface PairingApproveRequest {
  pairingCode: string;
  patientId:   string;
}

export interface VitalsNotification {
  deviceId:  string;
  patientId: string;
  heartRate: number;
  spO2:      number;
  steps:     number;
  timestamp: string;
}