import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/layout/Layout';
import PatientMap     from '../components/dashboard/PatientMap';
import DeviceStatus   from '../components/dashboard/DeviceStatus';
import HeartRateChart from '../components/dashboard/HeartRateChart';
import AlertItem      from '../components/alerts/AlertItem';
import { useAuth }    from '../hooks/useAuth';
import { useWebSocket } from '../hooks/useWebSocket';
import { patientsApi }  from '../api/patients';
import { alertsApi }    from '../api/alerts';
import { biometricApi } from '../api/biometric';
import { safeZonesApi } from '../api/safezones';
import { gpsApi } from '../api/gps';
import type {
  PatientResponse,
  AlertResponse,
  BiometricHistoryResponse,
  SafeZoneResponse,
  SmartwatchResponse,
  VitalsNotification,
  LocationNotification,
} from '../types';
import { MapPin, Plus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import ConnectionStatus   from '../components/dashboard/ConnectionStatus';
import { smartwatchesApi } from '../api/smartwatches';

const DashboardPage = () => {
  const { caregiver }  = useAuth();
  const navigate       = useNavigate();

  const [patients,   setPatients]   = useState<PatientResponse[]>([]);
  const [selected,   setSelected]   = useState<PatientResponse | null>(null);
  const [alerts,     setAlerts]     = useState<AlertResponse[]>([]);
  const [records,    setRecords]    = useState<BiometricHistoryResponse[]>([]);
  const [safeZones,  setSafeZones]  = useState<SafeZoneResponse[]>([]);
  const [lastLocation, setLastLocation] = useState<{
    latitude: number; longitude: number
  } | null>(null);
  const [loading, setLoading] = useState(true);

  const [devices, setDevices] = useState<SmartwatchResponse[]>([]);

  // Cargar pacientes del cuidador
  useEffect(() => {
    patientsApi.getAll().then(({ data }) => {
      setPatients(data);
      if (data.length > 0) setSelected(data[0]);
      setLoading(false);
    });
  }, []);

  // Cargar datos del paciente seleccionado
  useEffect(() => {
    if (!selected?.patientId) return;

    const controller = new AbortController();

    alertsApi.getByPatient(selected.patientId, 'ACTIVE')
      .then(({ data }) => {
        if (!controller.signal.aborted) setAlerts(data.content);
      })
      .catch(() => {
        if (!controller.signal.aborted) setAlerts([]);
      });

    if (selected.activeDeviceId) {
      biometricApi.getRecentByDevice(selected.activeDeviceId)
        .then(({ data }) => {
          if (!controller.signal.aborted) setRecords(data);
        })
        .catch(() => {
          if (!controller.signal.aborted) setRecords([]);
        });

      gpsApi.getLatestByDevice(selected.activeDeviceId)
        .then(({ data }) => {
          if (!controller.signal.aborted) setLastLocation({
            latitude:  data.latitude,
            longitude: data.longitude,
          });
        })
        .catch(() => {
          if (!controller.signal.aborted) setLastLocation(null);
        });
    } else {
      Promise.resolve().then(() => {
        if (!controller.signal.aborted) {
          setRecords([]);
          setLastLocation(null);
        }
      });
    }
    safeZonesApi.getByPatient(selected.patientId)
      .then(({ data }) => {
        if (!controller.signal.aborted) setSafeZones(data);
      })
      .catch(() => {
        if (!controller.signal.aborted) setSafeZones([]);
      });

    return () => controller.abort();

  }, [selected]);

  useEffect(() => {
    const fetchDevices = () => {
      smartwatchesApi.getMyDevices()
        .then(({ data }) => setDevices(data))
        .catch(() => {});
    };

  fetchDevices();
    // Refrescar cada 30 segundos
    const interval = setInterval(fetchDevices, 30_000);
    return () => clearInterval(interval);
  }, []);

  const handleDeviceSelect = (deviceId: string) => {
    const patient = patients.find(p => p.activeDeviceId === deviceId);
    if (patient) setSelected(patient);
  };

  const handleNewAlert = useCallback((alert: AlertResponse) => {
  // Actualizar contador de alertas en la lista de pacientes
  setPatients((prev) => prev.map((p) => {
    if (p.patientId !== alert.patientId) return p;
    if (alert.status === 'ACTIVE') {
      return { ...p, activeAlertsCount: p.activeAlertsCount + 1 };
    } else {
      return { ...p, activeAlertsCount: Math.max(0, p.activeAlertsCount - 1) };
    }
  }));

  // Actualizar alertas del paciente seleccionado
  if (selected && alert.patientId === selected.patientId) {
    if (alert.status === 'ACTIVE') {
      setAlerts((prev) => {
        const exists = prev.find(a => a.alertId === alert.alertId);
        if (exists) return prev;
        return [alert, ...prev];
      });
    } else {
      setAlerts((prev) => prev.filter(a => a.alertId !== alert.alertId));
    }
  }
}, [selected]);

  const handleAlertResolved = (alertId: string) => {
    setAlerts((prev) => prev.filter((a) => a.alertId !== alertId));
  };

  const handleNewVitals = useCallback((vitals: VitalsNotification) => {
    if (selected && vitals.patientId === selected.patientId) {
      const newRecord: BiometricHistoryResponse = {
        recordId:    crypto.randomUUID(),
        deviceId:    vitals.deviceId,
        heartRate:   vitals.heartRate,
        spO2:        vitals.spO2,
        steps:       vitals.steps,
        temperature: null,
        timestamp:   vitals.timestamp,
      };
      setRecords((prev) => [newRecord, ...prev].slice(0, 10));
    }
  }, [selected]);

  const handleNewLocation = useCallback((location: LocationNotification) => {
      if (selected && location.patientId === selected.patientId) {
        setLastLocation({
          latitude:  location.latitude,
          longitude: location.longitude,
        });
      }
    }, [selected]);

    useWebSocket({
      caregiverId: caregiver?.caregiverId ?? '',
      onAlert:     handleNewAlert,
      onVitals:    handleNewVitals,
      onLocation:  handleNewLocation,
      enabled:     !!caregiver,
    });

  useWebSocket({
    caregiverId: caregiver?.caregiverId ?? '',
    onAlert:     handleNewAlert,
    onVitals:    handleNewVitals,
    enabled:     !!caregiver,
  });

  const deviceStatus: SmartwatchResponse | null = selected?.activeDeviceId
    ? devices.find(d => d.deviceId === selected.activeDeviceId) ?? null
    : null;

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-64 text-[#7F8C8D]">
          Cargando...
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      {/* Selector de paciente */}
      {patients.length > 1 && (
        <div className="flex gap-2 mb-6 flex-wrap">
          {patients.map((p) => (
            <button
              key={p.patientId}
              onClick={() => setSelected(p)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
                ${selected?.patientId === p.patientId
                  ? 'bg-[#1A8C7A] text-white'
                  : 'bg-white border border-gray-200 text-[#2C3E50] hover:bg-gray-50'
                }`}
            >
              {p.fullName}
              {p.activeAlertsCount > 0 && (
                <span className="ml-2 bg-[#CC2222] text-white text-xs
                                 rounded-full px-1.5 py-0.5">
                  {p.activeAlertsCount}
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {selected ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

          {/* ── Columna izquierda ── */}
          <div className="space-y-6">

            {/* Mapa */}
            <div>
              <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                             tracking-wide mb-3 flex items-center gap-2">
                <MapPin size={14} />
                Ubicación en tiempo real
              </h2>
              <PatientMap
                latitude={lastLocation?.latitude ?? null}
                longitude={lastLocation?.longitude ?? null}
                safeZones={safeZones}
                patientName={selected.fullName}
              />
              <button
                onClick={() => navigate(`/safe-zones/${selected.patientId}`)}
                className="mt-3 flex items-center gap-2 text-sm text-[#1A8C7A]
                           hover:text-[#126B5E] font-medium transition-all"
              >
                <MapPin size={16} className="text-[#1A8C7A] rounded-full" />
                Administrar zonas seguras
              </button>
            </div>

            {/* Alertas pendientes */}
            <div>
              <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                             tracking-wide mb-3">
                Alertas pendientes
              </h2>
              <div className="bg-white border border-gray-200 rounded-xl px-4">
                {alerts.length === 0 ? (
                  <p className="text-sm text-[#7F8C8D] py-4 text-center">
                    Sin alertas activas ✓
                  </p>
                ) : (
                  alerts.map((alert) => (
                    <AlertItem
                      key={alert.alertId}
                      alert={alert}
                      onResolved={handleAlertResolved}
                    />
                  ))
                )}
              </div>
            </div>
          </div>

          {/* ── Columna derecha ── */}
          <div className="space-y-6">

            <div>
                <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                              tracking-wide mb-3">
                  Estado de conexión
                </h2>
                <ConnectionStatus
                  devices={devices}
                  onSelect={handleDeviceSelect}
                  selectedId={selected?.activeDeviceId ?? null}
                />
            </div>

            {/* Estado del dispositivo */}
            <div>
              <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                             tracking-wide mb-3">
                Estado del dispositivo
              </h2>
              <DeviceStatus device={deviceStatus} />
            </div>

            {/* Gráfica FC */}
            <div>
              <h2 className="text-sm font-semibold text-[#7F8C8D] uppercase
                             tracking-wide mb-3">
                Frecuencia cardíaca
              </h2>
              <div className="bg-white border border-gray-200 rounded-xl p-4">
                <HeartRateChart records={records} />
                <p className="text-xs text-[#7F8C8D] mt-2 text-center">
                  — FC (bpm)  ·  ·  SpO2 (%)
                </p>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center h-64
                        text-[#7F8C8D] gap-3">
          <p className="text-lg">No tienes pacientes asignados</p>
          <button
            onClick={() => navigate('/patients')}
            className="text-[#1A8C7A] hover:underline text-sm"
          >
            Ir a gestión de pacientes →
          </button>
        </div>
      )}
    </Layout>
  );
};

export default DashboardPage;