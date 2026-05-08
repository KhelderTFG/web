import { useEffect, useState } from 'react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import { reportsApi  } from '../api/reports';
import { patientsApi } from '../api/patients';
import type { PatientResponse } from '../types';
import { FileText, Download, FileSpreadsheet } from 'lucide-react';
import { format, subDays, subMonths, startOfMonth, endOfMonth } from 'date-fns';
import { es } from 'date-fns/locale';

type FormatType = 'pdf' | 'csv';

type RangePreset = 'last7' | 'last30' | 'thisMonth' | 'lastMonth' | 'custom';

const RANGE_PRESETS: { label: string; value: RangePreset }[] = [
  { label: 'Últimos 7 días',   value: 'last7'     },
  { label: 'Últimos 30 días',  value: 'last30'    },
  { label: 'Este mes',         value: 'thisMonth' },
  { label: 'Mes anterior',     value: 'lastMonth' },
  { label: 'Personalizado',    value: 'custom'    },
];

const getRangeDates = (preset: RangePreset): { from: string; to: string } => {
  const now  = new Date();
  const fmt  = (d: Date) => d.toISOString();

  switch (preset) {
    case 'last7':
      return { from: fmt(subDays(now, 7)),         to: fmt(now) };
    case 'last30':
      return { from: fmt(subDays(now, 30)),        to: fmt(now) };
    case 'thisMonth':
      return { from: fmt(startOfMonth(now)),       to: fmt(now) };
    case 'lastMonth': {
      const lastMonth = subMonths(now, 1);
      return {
        from: fmt(startOfMonth(lastMonth)),
        to:   fmt(endOfMonth(lastMonth)),
      };
    }
    default:
      return { from: fmt(subDays(now, 7)), to: fmt(now) };
  }
};

const ReportsPage = () => {
  const [patients,    setPatients]    = useState<PatientResponse[]>([]);
  const [patientId,   setPatientId]   = useState('');
  const [preset,      setPreset]      = useState<RangePreset>('last7');
  const [customFrom,  setCustomFrom]  = useState('');
  const [customTo,    setCustomTo]    = useState('');
  const [loading,     setLoading]     = useState(false);
  const [loadingCsv,  setLoadingCsv]  = useState(false);
  const [error,       setError]       = useState('');
  const [lastReport,  setLastReport]  = useState<{
    name: string; format: FormatType; date: Date
  } | null>(null);

  useEffect(() => {
    patientsApi.getAll().then(({ data }) => {
      setPatients(data);
      if (data.length > 0) setPatientId(data[0].patientId);
    });
  }, []);

  const getDateRange = () => {
    if (preset === 'custom') {
      return {
        from: customFrom ? new Date(customFrom).toISOString() : '',
        to:   customTo   ? new Date(customTo).toISOString()   : '',
      };
    }
    return getRangeDates(preset);
  };

  const handleDownload = async (fmt: FormatType) => {
    const { from, to } = getDateRange();

    if (!patientId) {
      setError('Selecciona un paciente');
      return;
    }
    if (!from || !to) {
      setError('Selecciona un rango de fechas válido');
      return;
    }

    setError('');
    fmt === 'pdf' ? setLoading(true) : setLoadingCsv(true);

    try {
      const { data, headers } = await reportsApi.generate(
        patientId, from, to, fmt
      );

      // Extraer nombre del fichero de la cabecera Content-Disposition
      const disposition = headers['content-disposition'] ?? '';
      const match = disposition.match(/filename=(.+)/);
      const filename = match
        ? match[1]
        : `informe_khelder_${format(new Date(), 'yyyyMMdd')}.${fmt}`;

      // Descargar el blob
      const url  = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement('a');
      link.href  = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      setLastReport({ name: filename, format: fmt, date: new Date() });

    } catch {
      setError('Error al generar el informe. Verifica que hay datos en el período seleccionado.');
    } finally {
      setLoading(false);
      setLoadingCsv(false);
    }
  };

  const selectedPatient = patients.find(p => p.patientId === patientId);
  const { from, to }    = getDateRange();

  const fromLabel = from
    ? format(new Date(from), "d 'de' MMMM yyyy", { locale: es })
    : '--';
  const toLabel = to
    ? format(new Date(to),   "d 'de' MMMM yyyy", { locale: es })
    : '--';

  return (
    <Layout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[#2C3E50]">Informes médicos</h1>
        <p className="text-sm text-[#7F8C8D] mt-1">
          Genera y descarga informes biométricos en PDF o CSV
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* ── Columna izquierda: configuración ── */}
        <div className="space-y-6">

          {/* Selector de paciente */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h2 className="font-semibold text-[#2C3E50] mb-4">Paciente</h2>
            <div className="space-y-2">
              {patients.map((p) => (
                <button
                  key={p.patientId}
                  onClick={() => setPatientId(p.patientId)}
                  className={`w-full flex items-center justify-between px-4 py-3
                              rounded-xl border text-sm transition-all
                              ${patientId === p.patientId
                                ? 'border-[#1A8C7A] bg-[#1A8C7A]/5 text-[#1A8C7A]'
                                : 'border-gray-200 text-[#2C3E50] hover:bg-gray-50'
                              }`}
                >
                  <span className="font-medium">{p.fullName}</span>
                  <span className="text-xs text-[#7F8C8D]">{p.age} años</span>
                </button>
              ))}
            </div>
          </div>

          {/* Selector de rango */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h2 className="font-semibold text-[#2C3E50] mb-4">
              Período del informe
            </h2>

            <div className="space-y-2 mb-4">
              {RANGE_PRESETS.map((r) => (
                <button
                  key={r.value}
                  onClick={() => setPreset(r.value)}
                  className={`w-full text-left px-4 py-2.5 rounded-xl border
                              text-sm transition-all
                              ${preset === r.value
                                ? 'border-[#1A8C7A] bg-[#1A8C7A]/5 text-[#1A8C7A] font-medium'
                                : 'border-gray-200 text-[#2C3E50] hover:bg-gray-50'
                              }`}
                >
                  {r.label}
                </button>
              ))}
            </div>

            {/* Rango personalizado */}
            {preset === 'custom' && (
              <div className="grid grid-cols-2 gap-3 pt-3 border-t border-gray-100">
                <div>
                  <label className="text-xs font-medium text-gray-600 block mb-1">
                    Desde
                  </label>
                  <input
                    type="datetime-local"
                    value={customFrom}
                    onChange={(e) => setCustomFrom(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg
                               text-sm outline-none focus:ring-2
                               focus:ring-[#1A8C7A] focus:border-[#1A8C7A]"
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600 block mb-1">
                    Hasta
                  </label>
                  <input
                    type="datetime-local"
                    value={customTo}
                    onChange={(e) => setCustomTo(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg
                               text-sm outline-none focus:ring-2
                               focus:ring-[#1A8C7A] focus:border-[#1A8C7A]"
                  />
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ── Columna derecha: resumen y descarga ── */}
        <div className="space-y-6">

          {/* Resumen del informe */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h2 className="font-semibold text-[#2C3E50] mb-4">
              Resumen del informe
            </h2>

            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-[#7F8C8D]">Paciente</span>
                <span className="font-medium text-[#2C3E50]">
                  {selectedPatient?.fullName ?? '--'}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-[#7F8C8D]">Desde</span>
                <span className="font-medium text-[#2C3E50]">{fromLabel}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-[#7F8C8D]">Hasta</span>
                <span className="font-medium text-[#2C3E50]">{toLabel}</span>
              </div>
            </div>

            {error && (
              <div className="mt-4 bg-red-50 border border-red-200 rounded-lg
                              px-3 py-2 text-xs text-red-700">
                {error}
              </div>
            )}

            {/* Botones de descarga */}
            <div className="mt-6 space-y-3">
              <Button
                onClick={() => handleDownload('pdf')}
                loading={loading}
                className="w-full py-3"
              >
                <FileText size={16} />
                Descargar PDF
              </Button>
              <Button
                variant="secondary"
                onClick={() => handleDownload('csv')}
                loading={loadingCsv}
                className="w-full py-3"
              >
                <FileSpreadsheet size={16} />
                Descargar CSV
              </Button>
            </div>
          </div>

          {/* Último informe generado */}
          {lastReport && (
            <div className="bg-green-50 border border-green-200 rounded-2xl p-5">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-[#1A8C7A] rounded-xl flex items-center
                                justify-center flex-shrink-0">
                  {lastReport.format === 'pdf'
                    ? <FileText size={18} className="text-white" />
                    : <FileSpreadsheet size={18} className="text-white" />
                  }
                </div>
                <div>
                  <p className="text-sm font-medium text-[#1A8C7A]">
                    Informe generado correctamente
                  </p>
                  <p className="text-xs text-[#7F8C8D] mt-0.5">
                    {lastReport.name}
                  </p>
                  <p className="text-xs text-[#7F8C8D]">
                    {format(lastReport.date, "HH:mm 'del' d 'de' MMMM", {
                      locale: es,
                    })}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Info sobre el contenido */}
          <div className="bg-gray-50 border border-gray-200 rounded-2xl p-5">
            <h3 className="text-sm font-semibold text-[#2C3E50] mb-3">
              El informe incluye
            </h3>
            <ul className="space-y-1.5 text-xs text-[#7F8C8D]">
              {[
                '📋 Datos personales e historial médico',
                '❤️ Registros de frecuencia cardíaca',
                '🫁 Niveles de SpO2',
                '👣 Actividad y pasos',
                '🌡️ Temperatura corporal',
                '📊 Estadísticas del período (FC media, máxima y mínima)',
              ].map((item) => (
                <li key={item} className="flex items-center gap-2">
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default ReportsPage;