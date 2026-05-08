import { useEffect, useState } from 'react';
import type { SmartwatchResponse } from '../../types';
import { Wifi, WifiOff, Battery, Clock } from 'lucide-react';

interface Props {
  devices:   SmartwatchResponse[];
  onSelect:  (deviceId: string) => void;
  selectedId: string | null;
}

const ConnectionStatus = ({ devices, onSelect, selectedId }: Props) => {
  if (devices.length === 0) {
    return (
      <div className="bg-white border border-gray-200 rounded-xl p-4
                      text-sm text-[#7F8C8D] text-center">
        Sin dispositivos registrados
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {devices.map((device) => (
        <DeviceRow
          key={device.deviceId}
          device={device}
          isSelected={device.deviceId === selectedId}
          onClick={() => onSelect(device.deviceId)}
        />
      ))}
    </div>
  );
};

const DeviceRow = ({
  device,
  isSelected,
  onClick,
}: {
  device:     SmartwatchResponse;
  isSelected: boolean;
  onClick:    () => void;
}) => {
  // Semáforo de batería
  const batteryColor = (level: number | null) => {
    if (level === null) return 'text-gray-400';
    if (level < 15)     return 'text-[#CC2222]';
    if (level < 30)     return 'text-[#E67E22]';
    return 'text-[#1A8C7A]';
  };

  // Tiempo desde último ping en formato legible
  const pingLabel = (minutes: number | null) => {
    if (minutes === null) return 'desconocido';
    if (minutes < 1)  return 'ahora mismo';
    if (minutes < 60) return `hace ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    return `hace ${hours}h`;
  };

  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center justify-between p-3 rounded-xl
                  border transition-all text-left
                  ${isSelected
                    ? 'border-[#1A8C7A] bg-[#1A8C7A]/5'
                    : 'border-gray-200 bg-white hover:bg-gray-50'
                  }`}
    >
      <div className="flex items-center gap-3">
        {/* Indicador de conexión */}
        <div className={`w-2.5 h-2.5 rounded-full flex-shrink-0
          ${device.connectionStatus
            ? 'bg-[#1A8C7A] shadow-[0_0_6px_#1A8C7A]'
            : 'bg-[#CC2222]'
          }`}
        />

        <div>
          <p className="text-sm font-medium text-[#2C3E50]">
            {device.patientName}
          </p>
          <div className="flex items-center gap-2 mt-0.5">
            <span className={`flex items-center gap-1 text-xs
                             ${device.connectionStatus
                               ? 'text-[#1A8C7A]'
                               : 'text-[#CC2222]'}`}>
              {device.connectionStatus
                ? <><Wifi size={10} /> Conectado</>
                : <><WifiOff size={10} /> Desconectado</>
              }
            </span>
            <span className="text-gray-300">·</span>
            <span className="flex items-center gap-1 text-xs text-[#7F8C8D]">
              <Clock size={10} />
              {pingLabel(device.minutesSinceLastPing)}
            </span>
          </div>
        </div>
      </div>

      {/* Batería */}
      <div className={`flex items-center gap-1 text-xs font-medium
                       ${batteryColor(device.batteryLevel)}`}>
        <Battery size={14} />
        {device.batteryLevel !== null ? `${device.batteryLevel}%` : '--'}
      </div>
    </button>
  );
};

export default ConnectionStatus;