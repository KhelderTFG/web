import type { SmartwatchResponse } from '../../types';
import { Wifi, WifiOff, Battery } from 'lucide-react';

interface Props {
  device: SmartwatchResponse | null;
}

const BatteryBar = ({ level }: { level: number }) => {
  const color = level < 15
    ? 'bg-[#CC2222]'
    : level < 30
    ? 'bg-[#E67E22]'
    : 'bg-[#1A8C7A]';

  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
        <div
          className={`h-full ${color} rounded-full transition-all`}
          style={{ width: `${level}%` }}
        />
      </div>
      <span className="text-xs text-[#7F8C8D] w-8">{level}%</span>
    </div>
  );
};

const DeviceStatus = ({ device }: Props) => {
  if (!device) {
    return (
      <div className="bg-gray-50 rounded-xl p-4 text-sm text-[#7F8C8D]">
        Sin dispositivo asignado
      </div>
    );
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-4 space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-[#2C3E50] uppercase tracking-wide">
          {device.patientName}
        </p>
        {device.connectionStatus ? (
          <span className="flex items-center gap-1 text-xs text-[#1A8C7A]">
            <Wifi size={14} /> Conectado
          </span>
        ) : (
          <span className="flex items-center gap-1 text-xs text-[#CC2222]">
            <WifiOff size={14} /> Desconectado
          </span>
        )}
      </div>

      <div>
        <p className="text-xs text-[#7F8C8D] mb-1 flex items-center gap-1">
          <Battery size={12} /> Batería
        </p>
        <BatteryBar level={device.batteryLevel ?? 0} />
      </div>

      <p className="text-xs text-[#7F8C8D]">
        Última conexión:{' '}
        {device.minutesSinceLastPing !== null
          ? `hace ${device.minutesSinceLastPing} min`
          : 'desconocida'}
      </p>
    </div>
  );
};

export default DeviceStatus;