import type { AlertResponse } from '../../types';
import { alertsApi } from '../../api/alerts';
import { useState } from 'react';

const ALERT_LABELS: Record<string, string> = {
  SOS_MANUAL:      '🆘 SOS Manual',
  FALL_DETECTED:   '⚠️ Caída detectada',
  HEART_RATE_HIGH: '❤️ FC elevada',
  HEART_RATE_LOW:  '💙 FC baja',
  BATTERY_LOW:     '🔋 Batería crítica',
  GEOFENCE_EXIT:   '📍 Fuera de zona',
  SPO2_LOW:        '🫁 SpO2 baja',
};

interface Props {
  alert:     AlertResponse;
  onResolved: (alertId: string) => void;
}

const AlertItem = ({ alert, onResolved }: Props) => {
  const [loading, setLoading] = useState(false);

  const time = new Date(alert.timestamp).toLocaleTimeString('es-ES', {
    hour:   '2-digit',
    minute: '2-digit',
  });

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
    <div className="flex items-center justify-between py-3 border-b
                    border-gray-100 last:border-0">
      <div>
        <p className="text-sm font-medium text-[#2C3E50]">
          {time} — {ALERT_LABELS[alert.alertType] ?? alert.alertType}
        </p>
        {alert.patientName && (
          <p className="text-xs text-[#7F8C8D]">{alert.patientName}</p>
        )}
      </div>
      <button
        onClick={handleResolve}
        disabled={loading}
        className="px-3 py-1.5 text-xs font-medium bg-[#1A8C7A] text-white
                   rounded-lg hover:bg-[#126B5E] disabled:opacity-50
                   transition-all"
      >
        {loading ? 'Resolviendo...' : 'Marcar como resuelta'}
      </button>
    </div>
  );
};

export default AlertItem;