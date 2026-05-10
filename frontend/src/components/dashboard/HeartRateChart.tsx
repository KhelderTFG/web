import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, ReferenceLine,
} from 'recharts';
import type { BiometricHistoryResponse } from '../../types';

interface Props {
  records: BiometricHistoryResponse[];
}

const HeartRateChart = ({ records }: Props) => {
  if (records.length === 0) {
    return (
      <div className="h-48 flex items-center justify-center
                      text-sm text-[#7F8C8D]">
        Sin datos de frecuencia cardíaca
      </div>
    );
  }

  const data = [...records].reverse().map((r) => ({
    time: new Date(r.timestamp).toLocaleTimeString('es-ES', {
      hour: '2-digit', minute: '2-digit',
    }),
    fc:   r.heartRate !== null ? Math.round(r.heartRate) : null,
    spo2: r.spO2      !== null ? Math.round(r.spO2)      : null,
  }));

  return (
    <ResponsiveContainer width="100%" height={200}>
      <LineChart data={data} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
        <XAxis dataKey="time" tick={{ fontSize: 10 }} />
        <YAxis tick={{ fontSize: 10 }} domain={[30, 160]} />
        <Tooltip
          contentStyle={{ fontSize: 12, borderRadius: 8 }}
          formatter={(value, name) => [
            `${value} ${name === 'fc' ? 'bpm' : '%'}`,
            name === 'fc' ? 'FC' : 'SpO2',
          ]}
        />
        {/* Umbrales clínicos */}
        <ReferenceLine y={140} stroke="#CC2222" strokeDasharray="3 3"
          label={{ value: '140', fontSize: 10, fill: '#CC2222' }} />
        <ReferenceLine y={45}  stroke="#CC2222" strokeDasharray="3 3"
          label={{ value: '45',  fontSize: 10, fill: '#CC2222' }} />

        <Line type="monotone" dataKey="fc"   stroke="#1A8C7A"
              strokeWidth={2} dot={false} connectNulls />
        <Line type="monotone" dataKey="spo2" stroke="#2AB5A0"
              strokeWidth={2} dot={false} connectNulls strokeDasharray="4 2" />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default HeartRateChart;