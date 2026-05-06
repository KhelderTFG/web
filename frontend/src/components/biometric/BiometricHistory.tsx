import { useEffect, useState } from 'react';
import {
  LineChart, Line, AreaChart, Area,
  XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine, Legend,
} from 'recharts';
import { biometricApi } from '../../api/biometric';
import type { BiometricHistoryResponse } from '../../types';
import { format, subDays, startOfDay, endOfDay } from 'date-fns';
import { es } from 'date-fns/locale';

interface Props {
  patientId: string;
  deviceId:  string;
}

type Range = '24h' | '7d' | '30d';

const RANGES: { label: string; value: Range }[] = [
  { label: 'Últimas 24h', value: '24h' },
  { label: 'Últimos 7 días', value: '7d'  },
  { label: 'Últimos 30 días', value: '30d' },
];

const BiometricHistory = ({ patientId, deviceId }: Props) => {
  const [records,  setRecords]  = useState<BiometricHistoryResponse[]>([]);
  const [loading,  setLoading]  = useState(true);
  const [range,    setRange]    = useState<Range>('24h');
  const [page,     setPage]     = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const PAGE_SIZE = 50;

  useEffect(() => {
    setLoading(true);
    setPage(0);

    const now  = new Date();
    const from = range === '24h'
      ? subDays(now, 1)
      : range === '7d'
      ? subDays(now, 7)
      : subDays(now, 30);

    biometricApi.getPatientHistory(
      patientId,
      from.toISOString(),
      now.toISOString(),
      0,
      PAGE_SIZE
    ).then(({ data }) => {
      setRecords(data.content);
      setTotalPages(data.totalPages);
    }).finally(() => setLoading(false));

  }, [patientId, range]);

  // Datos para las gráficas
  const chartData = [...records].reverse().map((r) => ({
    time: format(new Date(r.timestamp), 'dd/MM HH:mm', { locale: es }),
    fc:   r.heartRate   !== null ? +r.heartRate.toFixed(1)   : null,
    spo2: r.spO2        !== null ? +r.spO2.toFixed(1)        : null,
    temp: r.temperature !== null ? +r.temperature.toFixed(1) : null,
    pasos: r.steps      ?? null,
  }));

  // Estadísticas
  const fcValues   = records.filter(r => r.heartRate !== null).map(r => r.heartRate!);
  const spo2Values = records.filter(r => r.spO2 !== null).map(r => r.spO2!);
  const totalSteps = records.reduce((acc, r) => acc + (r.steps ?? 0), 0);

  const stats = {
    fcMedia: fcValues.length
      ? (fcValues.reduce((a, b) => a + b, 0) / fcValues.length).toFixed(0)
      : '--',
    fcMax: fcValues.length ? Math.max(...fcValues).toFixed(0) : '--',
    fcMin: fcValues.length ? Math.min(...fcValues).toFixed(0) : '--',
    spo2Media: spo2Values.length
      ? (spo2Values.reduce((a, b) => a + b, 0) / spo2Values.length).toFixed(1)
      : '--',
    totalSteps,
  };

  return (
    <div className="space-y-6">

      {/* Selector de rango */}
      <div className="flex items-center justify-between">
        <div className="flex gap-1 bg-gray-100 p-1 rounded-xl">
          {RANGES.map((r) => (
            <button
              key={r.value}
              onClick={() => setRange(r.value)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
                ${range === r.value
                  ? 'bg-white text-[#2C3E50] shadow-sm'
                  : 'text-[#7F8C8D] hover:text-[#2C3E50]'
                }`}
            >
              {r.label}
            </button>
          ))}
        </div>
        <p className="text-sm text-[#7F8C8D]">
          {records.length} registros
        </p>
      </div>

      {/* Tarjetas de estadísticas */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {[
          { label: 'FC media',  value: `${stats.fcMedia} bpm`,  color: '#1A8C7A' },
          { label: 'FC máxima', value: `${stats.fcMax} bpm`,    color: '#CC2222' },
          { label: 'FC mínima', value: `${stats.fcMin} bpm`,    color: '#2AB5A0' },
          { label: 'SpO2 media',value: `${stats.spo2Media}%`,   color: '#1A8C7A' },
          { label: 'Pasos',     value: stats.totalSteps.toLocaleString('es-ES'),
            color: '#E67E22' },
        ].map((stat) => (
          <div key={stat.label}
               className="bg-white border border-gray-200 rounded-xl p-4 text-center">
            <p className="text-xs text-[#7F8C8D] mb-1">{stat.label}</p>
            <p className="text-lg font-bold" style={{ color: stat.color }}>
              {stat.value}
            </p>
          </div>
        ))}
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-48 text-[#7F8C8D]">
          Cargando registros...
        </div>
      ) : records.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-2xl p-12
                        text-center text-[#7F8C8D]">
          Sin registros en el período seleccionado
        </div>
      ) : (
        <>
          {/* Gráfica FC */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h3 className="font-semibold text-[#2C3E50] mb-4">
              Frecuencia cardíaca (bpm)
            </h3>
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={chartData}
                         margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="fcGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#1A8C7A" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#1A8C7A" stopOpacity={0}   />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }}
                       interval="preserveStartEnd" />
                <YAxis tick={{ fontSize: 10 }} domain={[30, 160]} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }}
                  formatter={(v: number) => [`${v} bpm`, 'FC']} />
                <ReferenceLine y={140} stroke="#CC2222" strokeDasharray="3 3" />
                <ReferenceLine y={45}  stroke="#CC2222" strokeDasharray="3 3" />
                <Area type="monotone" dataKey="fc" stroke="#1A8C7A"
                      fill="url(#fcGrad)" strokeWidth={2}
                      dot={false} connectNulls />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Gráfica SpO2 */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h3 className="font-semibold text-[#2C3E50] mb-4">
              Saturación de oxígeno — SpO2 (%)
            </h3>
            <ResponsiveContainer width="100%" height={180}>
              <AreaChart data={chartData}
                         margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="spo2Grad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#2AB5A0" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#2AB5A0" stopOpacity={0}   />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }}
                       interval="preserveStartEnd" />
                <YAxis tick={{ fontSize: 10 }} domain={[85, 100]} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }}
                  formatter={(v: number) => [`${v}%`, 'SpO2']} />
                <ReferenceLine y={90} stroke="#CC2222" strokeDasharray="3 3" />
                <Area type="monotone" dataKey="spo2" stroke="#2AB5A0"
                      fill="url(#spo2Grad)" strokeWidth={2}
                      dot={false} connectNulls />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Gráfica pasos */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h3 className="font-semibold text-[#2C3E50] mb-4">
              Actividad — Pasos
            </h3>
            <ResponsiveContainer width="100%" height={180}>
              <AreaChart data={chartData}
                         margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="pasosGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#E67E22" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#E67E22" stopOpacity={0}   />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }}
                       interval="preserveStartEnd" />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8 }}
                  formatter={(v: number) => [v.toLocaleString('es-ES'), 'Pasos']} />
                <Area type="monotone" dataKey="pasos" stroke="#E67E22"
                      fill="url(#pasosGrad)" strokeWidth={2}
                      dot={false} connectNulls />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Paginación */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <button
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
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
                onClick={() => setPage(p => p + 1)}
                className="px-4 py-2 text-sm border border-gray-200 rounded-lg
                           disabled:opacity-40 hover:bg-gray-50 transition-all"
              >
                Siguiente →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default BiometricHistory;