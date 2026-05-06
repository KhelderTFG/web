import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/layout/Layout';
import { alertsApi } from '../api/alerts';
import { useAuth } from '../hooks/useAuth';
import { useWebSocket } from '../hooks/useWebSocket';
import type { AlertResponse, AlertStatus } from '../types';
import { Bell, BellOff, Filter } from 'lucide-react';

const ALERT_TYPE_LABELS: Record<string, string> = {
  SOS_MANUAL:      '🆘 SOS Manual',
  FALL_DETECTED:   '⚠️ Caída detectada',
  HEART_RATE_HIGH: '❤️ FC elevada',
  HEART_RATE_LOW:  '💙 FC baja',
  BATTERY_LOW:     '🔋 Batería crítica',
  GEOFENCE_EXIT:   '📍 Fuera de zona',
  SPO2_LOW:        '🫁 SpO2 baja',
};

type FilterType = AlertStatus;

const FILTERS: { label: string; value: FilterType }[] = [
  { label: 'Activas',    value: 'ACTIVE'    },
  { label: 'Resueltas',  value: 'RESOLVED'  },
  { label: 'Canceladas', value: 'CANCELLED' },
];

const AlertsPage = () => {
  const { caregiver } = useAuth();

  const [alerts,     setAlerts]     = useState<AlertResponse[]>([]);
  const [loading,    setLoading]    = useState(true);
  const [filter,     setFilter]     = useState<FilterType>('ACTIVE');
  const [page,       setPage]       = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Alertas recibidas por WebSocket — solo se muestran si el filtro es ACTIVE
  const [liveAlerts, setLiveAlerts] = useState<AlertResponse[]>([]);

  const PAGE_SIZE = 15;

  const fetchAlerts = useCallback((status: FilterType, p: number) => {
    setLoading(true);
    alertsApi.getMyAlerts(status, p, PAGE_SIZE)
      .then(({ data }) => {
        setAlerts(data.content);
        setTotalPages(data.totalPages);
      })
      .finally(() => setLoading(false));
  }, []);

  // Al cambiar filtro: resetear página, limpiar live alerts y recargar
  useEffect(() => {
    setPage(0);
    setLiveAlerts([]);
    fetchAlerts(filter, 0);
  }, [filter, fetchAlerts]);

  // WebSocket — solo añade alertas a liveAlerts, no toca alerts
  const handleNewAlert = useCallback((alert: AlertResponse) => {
    if (filter === 'ACTIVE') {
      setLiveAlerts((prev) => {
        // Evitar duplicados
        if (prev.some(a => a.alertId === alert.alertId)) return prev;
        return [alert, ...prev];
      });
    }
  }, [filter]);

  useWebSocket({
    caregiverId: caregiver?.caregiverId ?? '',
    onAlert:     handleNewAlert,
    enabled:     !!caregiver,
  });

  // Al resolver: eliminar de ambas listas y recargar para consistencia
  const handleAlertResolved = (alertId: string) => {
    setAlerts(prev => prev.filter(a => a.alertId !== alertId));
    setLiveAlerts(prev => prev.filter(a => a.alertId !== alertId));
  };

  const handleDismissLive = (alertId: string) => {
    setLiveAlerts(prev => prev.filter(a => a.alertId !== alertId));
  };

  // Live alerts van siempre primero, sin duplicar con alerts
  const displayAlerts = [
    ...liveAlerts,
    ...alerts.filter(a => !liveAlerts.some(l => l.alertId === a.alertId)),
  ];

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[#2C3E50]">Alertas</h1>
          <p className="text-sm text-[#7F8C8D] mt-1">
            Panel de gestión de alertas
          </p>
        </div>

        {liveAlerts.length > 0 && filter === 'ACTIVE' && (
          <div className="flex items-center gap-2 bg-red-50 border border-red-200
                          rounded-xl px-4 py-2">
            <Bell size={16} className="text-[#CC2222] animate-bounce" />
            <span className="text-sm font-medium text-[#CC2222]">
              {liveAlerts.length} nueva{liveAlerts.length !== 1 ? 's' : ''}
            </span>
          </div>
        )}
      </div>

      {/* Filtros */}
      <div className="flex items-center gap-2 mb-6">
        <Filter size={16} className="text-[#7F8C8D]" />
        <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
          {FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => setFilter(f.value)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
                ${filter === f.value
                  ? 'bg-white text-[#2C3E50] shadow-sm'
                  : 'text-[#7F8C8D] hover:text-[#2C3E50]'
                }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Lista */}
      {loading ? (
        <div className="flex items-center justify-center h-48 text-[#7F8C8D]">
          Cargando alertas...
        </div>
      ) : displayAlerts.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-2xl p-12
                        text-center">
          <BellOff size={40} className="text-gray-300 mx-auto mb-3" />
          <p className="text-[#7F8C8D]">No hay alertas en esta categoría</p>
        </div>
      ) : (
        <div className="space-y-3">
          {displayAlerts.map((alert) => (
            <AlertCard
              key={alert.alertId}
              alert={alert}
              isLive={liveAlerts.some(l => l.alertId === alert.alertId)}
              onResolved={handleAlertResolved}
              onDismiss={handleDismissLive}
            />
          ))}
        </div>
      )}

      {/* Paginación */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-6">
          <button
            disabled={page === 0}
            onClick={() => {
              const newPage = page - 1;
              setPage(newPage);
              fetchAlerts(filter, newPage);
            }}
            className="px-4 py-2 text-sm border border-gray-200 rounded-lg
                       disabled:opacity-40 hover:bg-gray-50 transition-all"
          >
            ← Anterior
          </button>
          <span className="text-sm text-[#7F8C8D]">
            Página {page + 1} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => {
              const newPage = page + 1;
              setPage(newPage);
              fetchAlerts(filter, newPage);
            }}
            className="px-4 py-2 text-sm border border-gray-200 rounded-lg
                       disabled:opacity-40 hover:bg-gray-50 transition-all"
          >
            Siguiente →
          </button>
        </div>
      )}
    </Layout>
  );
};

// ---- Tarjeta de alerta ----
interface AlertCardProps {
  alert:      AlertResponse;
  isLive:     boolean;
  onResolved: (alertId: string) => void;
  onDismiss:  (alertId: string) => void;
}

const AlertCard = ({ alert, isLive, onResolved, onDismiss }: AlertCardProps) => {
  const [loading, setLoading] = useState(false);

  const date    = new Date(alert.timestamp);
  const dateStr = date.toLocaleDateString('es-ES', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
  const timeStr = date.toLocaleTimeString('es-ES', {
    hour: '2-digit', minute: '2-digit',
  });

  const statusColors: Record<AlertStatus, string> = {
    ACTIVE:    'bg-red-50 border-red-200',
    RESOLVED:  'bg-green-50 border-green-200',
    CANCELLED: 'bg-gray-50 border-gray-200',
  };

  const statusBadge: Record<AlertStatus, string> = {
    ACTIVE:    'bg-[#CC2222] text-white',
    RESOLVED:  'bg-[#1A8C7A] text-white',
    CANCELLED: 'bg-gray-400 text-white',
  };

  const statusLabel: Record<AlertStatus, string> = {
    ACTIVE:    'Activa',
    RESOLVED:  'Resuelta',
    CANCELLED: 'Cancelada',
  };

  const handleResolve = async () => {
    setLoading(true);
    try {
      await alertsApi.resolve(alert.alertId);
      onResolved(alert.alertId);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`border rounded-2xl p-5 transition-all
      ${statusColors[alert.status]}
      ${isLive ? 'ring-2 ring-[#CC2222] ring-offset-2' : ''}
    `}>
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <div className="flex items-center gap-2 flex-wrap mb-2">
            <span className="font-semibold text-[#2C3E50]">
              {ALERT_TYPE_LABELS[alert.alertType] ?? alert.alertType}
            </span>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium
                             ${statusBadge[alert.status]}`}>
              {statusLabel[alert.status]}
            </span>
            {isLive && (
              <span className="text-xs px-2 py-0.5 rounded-full font-medium
                               bg-[#E67E22] text-white animate-pulse">
                Nueva
              </span>
            )}
          </div>

          {alert.patientName && (
            <p className="text-sm text-[#2C3E50] mb-1">
              Paciente: <span className="font-medium">{alert.patientName}</span>
            </p>
          )}

          <p className="text-xs text-[#7F8C8D]">
            {dateStr} a las {timeStr}
          </p>

          <div className="flex gap-4 mt-2 flex-wrap">
            {alert.heartRate && (
              <span className="text-xs text-[#7F8C8D]">
                ❤️ {alert.heartRate.toFixed(0)} bpm
              </span>
            )}
            {alert.batteryLevel && (
              <span className="text-xs text-[#7F8C8D]">
                🔋 {alert.batteryLevel}%
              </span>
            )}
            {alert.latitude && alert.longitude && (
              <span className="text-xs text-[#7F8C8D]">
                📍 {alert.latitude.toFixed(4)}, {alert.longitude.toFixed(4)}
              </span>
            )}
          </div>
        </div>

        <div className="flex flex-col gap-2 shrink-0">
          {alert.status === 'ACTIVE' && (
            <button
              onClick={handleResolve}
              disabled={loading}
              className="px-3 py-1.5 text-xs font-medium bg-[#1A8C7A] text-white
                         rounded-lg hover:bg-[#126B5E] disabled:opacity-50
                         transition-all"
            >
              {loading ? 'Resolviendo...' : 'Marcar resuelta'}
            </button>
          )}
          {isLive && (
            <button
              onClick={() => onDismiss(alert.alertId)}
              className="px-3 py-1.5 text-xs font-medium text-[#7F8C8D]
                         hover:bg-gray-100 rounded-lg transition-all"
            >
              Descartar
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default AlertsPage;