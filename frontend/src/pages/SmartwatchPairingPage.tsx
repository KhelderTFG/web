import { useState, useEffect } from 'react';
import Layout from '../components/layout/Layout';
import { smartwatchesApi } from '../api/smartwatches';
import { patientsApi } from '../api/patients';
import { useNavigate } from 'react-router-dom';
import { Smartphone, CheckCircle, AlertCircle, User } from 'lucide-react';
import type { PatientResponse } from '../types';

type Step = 'instructions' | 'patient' | 'code' | 'success' | 'error';

const SmartWatchPairingPage = () => {
  const navigate = useNavigate();
  const [step,     setStep]     = useState<Step>('instructions');
  const [code,     setCode]     = useState('');
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState<string | null>(null);
  const [patients, setPatients] = useState<PatientResponse[]>([]);
  const [selected, setSelected] = useState<PatientResponse | null>(null);
  const [loadingPatients, setLoadingPatients] = useState(false);

  useEffect(() => {
    if (step === 'patient') {
      setLoadingPatients(true);
      patientsApi.getAll()
        .then(({ data }) => setPatients(data))
        .catch(() => setPatients([]))
        .finally(() => setLoadingPatients(false));
    }
  }, [step]);

  const handleApprove = async () => {
    if (code.length !== 6) {
      setError('El código debe tener 6 dígitos.');
      return;
    }
    if (!selected) {
      setError('Selecciona un paciente.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      await smartwatchesApi.approvePairing({
        pairingCode: code,
        patientId:   selected.patientId
      });
      setStep('success');
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? 'Código inválido o expirado.';
      setError(msg);
      setStep('error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-lg mx-auto">

        <div className="mb-6">
          <h1 className="text-xl font-bold text-[#2C3E50]">
            Vincular smartwatch
          </h1>
          <p className="text-sm text-[#7F8C8D] mt-1">
            Asocia un Galaxy Watch al sistema Khelder
          </p>
        </div>

        {/* Indicador de pasos */}
        {(step === 'instructions' || step === 'patient' || step === 'code') && (
          <div className="flex items-center gap-2 mb-6">
            {['instructions', 'patient', 'code'].map((s, i) => (
              <div key={s} className="flex items-center gap-2">
                <div className={`w-7 h-7 rounded-full flex items-center justify-center
                                text-xs font-bold transition-colors
                                ${step === s
                                  ? 'bg-[#1A8C7A] text-white'
                                  : ['instructions', 'patient', 'code'].indexOf(step) > i
                                    ? 'bg-[#E8F5F3] text-[#1A8C7A]'
                                    : 'bg-gray-100 text-[#7F8C8D]'
                                }`}>
                  {i + 1}
                </div>
                {i < 2 && <div className="flex-1 h-px bg-gray-200 w-8" />}
              </div>
            ))}
            <span className="text-xs text-[#7F8C8D] ml-2">
              {step === 'instructions' && 'Instrucciones'}
              {step === 'patient'      && 'Seleccionar paciente'}
              {step === 'code'         && 'Introducir código'}
            </span>
          </div>
        )}

        <div className="bg-white border border-gray-200 rounded-xl p-6">

          {/* Paso 1 — Instrucciones */}
          {step === 'instructions' && (
            <div className="space-y-6">
              <div className="flex items-center justify-center">
                <div className="bg-[#E8F5F3] rounded-full p-4">
                  <Smartphone size={40} className="text-[#1A8C7A]" />
                </div>
              </div>
              <div className="space-y-4">
                <h2 className="text-base font-semibold text-[#2C3E50] text-center">
                  Instrucciones de vinculación
                </h2>
                <ol className="space-y-3 text-sm text-[#2C3E50]">
                  {[
                    'Abre la app Khelder en el Galaxy Watch.',
                    'Asegúrate de que el Watch está conectado al teléfono móvil del paciente via Bluetooth.',
                    'Anota el código de 6 dígitos que aparece en la pantalla del Watch.',
                    'Selecciona el paciente y pulsa Continuar.'
                  ].map((text, i) => (
                    <li key={i} className="flex gap-3">
                      <span className="flex-shrink-0 w-6 h-6 rounded-full bg-[#1A8C7A]
                                       text-white text-xs flex items-center justify-center
                                       font-bold">
                        {i + 1}
                      </span>
                      <span dangerouslySetInnerHTML={{ __html: text
                        .replace('Khelder', '<strong>Khelder</strong>')
                        .replace('código de 6 dígitos', '<strong>código de 6 dígitos</strong>')
                        .replace('Bluetooth', '<strong>Bluetooth</strong>')
                      }} />
                    </li>
                  ))}
                </ol>
              </div>
              <button
                onClick={() => setStep('patient')}
                className="w-full bg-[#1A8C7A] text-white rounded-lg py-2.5
                           text-sm font-medium hover:bg-[#126B5E] transition-colors"
              >
                Continuar
              </button>
            </div>
          )}

          {/* Paso 2 — Seleccionar paciente */}
          {step === 'patient' && (
            <div className="space-y-6">
              <div className="text-center space-y-1">
                <h2 className="text-base font-semibold text-[#2C3E50]">
                  Selecciona el paciente
                </h2>
                <p className="text-sm text-[#7F8C8D]">
                  ¿A qué paciente pertenece este smartwatch?
                </p>
              </div>

              {loadingPatients ? (
                <div className="text-center py-4 text-sm text-[#7F8C8D]">
                  Cargando pacientes...
                </div>
              ) : patients.length === 0 ? (
                <div className="text-center py-4 text-sm text-[#7F8C8D]">
                  No tienes pacientes registrados.{' '}
                  <button
                    onClick={() => navigate('/patients')}
                    className="text-[#1A8C7A] hover:underline"
                  >
                    Añadir paciente →
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  {patients.map((p) => (
                    <button
                      key={p.patientId}
                      onClick={() => setSelected(p)}
                      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg
                                  border text-left transition-all
                                  ${selected?.patientId === p.patientId
                                    ? 'border-[#1A8C7A] bg-[#E8F5F3]'
                                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                                  }`}
                    >
                      <div className={`w-8 h-8 rounded-full flex items-center
                                       justify-center flex-shrink-0
                                       ${selected?.patientId === p.patientId
                                         ? 'bg-[#1A8C7A] text-white'
                                         : 'bg-gray-100 text-[#7F8C8D]'
                                       }`}>
                        <User size={16} />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-[#2C3E50]">
                          {p.fullName}
                        </p>
                        <p className="text-xs text-[#7F8C8D]">
                          {p.activeDeviceId
                            ? 'Tiene smartwatch asignado'
                            : 'Sin smartwatch asignado'}
                        </p>
                      </div>
                      {selected?.patientId === p.patientId && (
                        <CheckCircle
                          size={16}
                          className="text-[#1A8C7A] ml-auto flex-shrink-0"
                        />
                      )}
                    </button>
                  ))}
                </div>
              )}

              <div className="flex gap-3">
                <button
                  onClick={() => { setStep('instructions'); setSelected(null); }}
                  className="flex-1 border border-gray-200 text-[#2C3E50] rounded-lg
                             py-2.5 text-sm font-medium hover:bg-gray-50 transition-colors"
                >
                  Atrás
                </button>
                <button
                  onClick={() => setStep('code')}
                  disabled={!selected}
                  className="flex-1 bg-[#1A8C7A] text-white rounded-lg py-2.5
                             text-sm font-medium hover:bg-[#126B5E] transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Continuar
                </button>
              </div>
            </div>
          )}

          {/* Paso 3 — Introducir código */}
          {step === 'code' && (
            <div className="space-y-6">
              <div className="text-center space-y-1">
                <h2 className="text-base font-semibold text-[#2C3E50]">
                  Introduce el código del Watch
                </h2>
                <p className="text-sm text-[#7F8C8D]">
                  Paciente: <strong className="text-[#2C3E50]">{selected?.fullName}</strong>
                </p>
                <p className="text-xs text-[#7F8C8D]">El código caduca en 10 minutos</p>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-medium text-[#7F8C8D] uppercase
                                  tracking-wide">
                  Código de 6 dígitos
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  value={code}
                  onChange={(e) => {
                    setError(null);
                    setCode(e.target.value.replace(/\D/g, ''));
                  }}
                  placeholder="000000"
                  className="w-full border border-gray-200 rounded-lg px-4 py-3
                             text-center text-2xl font-bold tracking-[0.5em]
                             text-[#2C3E50] focus:outline-none focus:border-[#1A8C7A]
                             focus:ring-1 focus:ring-[#1A8C7A] transition-colors"
                />
                {error && (
                  <p className="text-xs text-[#CC2222] flex items-center gap-1">
                    <AlertCircle size={12} />
                    {error}
                  </p>
                )}
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => { setStep('patient'); setCode(''); setError(null); }}
                  className="flex-1 border border-gray-200 text-[#2C3E50] rounded-lg
                             py-2.5 text-sm font-medium hover:bg-gray-50 transition-colors"
                >
                  Atrás
                </button>
                <button
                  onClick={handleApprove}
                  disabled={loading || code.length !== 6}
                  className="flex-1 bg-[#1A8C7A] text-white rounded-lg py-2.5
                             text-sm font-medium hover:bg-[#126B5E] transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? 'Vinculando...' : 'Vincular'}
                </button>
              </div>
            </div>
          )}

          {/* Éxito */}
          {step === 'success' && (
            <div className="space-y-6 text-center">
              <div className="flex items-center justify-center">
                <div className="bg-[#E8F5F3] rounded-full p-4">
                  <CheckCircle size={40} className="text-[#1A8C7A]" />
                </div>
              </div>
              <div className="space-y-1">
                <h2 className="text-base font-semibold text-[#2C3E50]">
                  Smartwatch vinculado
                </h2>
                <p className="text-sm text-[#7F8C8D]">
                  El dispositivo ha sido asociado correctamente a{' '}
                  <strong className="text-[#2C3E50]">{selected?.fullName}</strong>.
                  En unos segundos aparecerá en el dashboard.
                </p>
              </div>
              <button
                onClick={() => navigate('/')}
                className="w-full bg-[#1A8C7A] text-white rounded-lg py-2.5
                           text-sm font-medium hover:bg-[#126B5E] transition-colors"
              >
                Ir al dashboard
              </button>
            </div>
          )}

          {/* Error */}
          {step === 'error' && (
            <div className="space-y-6 text-center">
              <div className="flex items-center justify-center">
                <div className="bg-red-50 rounded-full p-4">
                  <AlertCircle size={40} className="text-[#CC2222]" />
                </div>
              </div>
              <div className="space-y-1">
                <h2 className="text-base font-semibold text-[#2C3E50]">
                  No se pudo vincular
                </h2>
                <p className="text-sm text-[#7F8C8D]">{error}</p>
              </div>
              <div className="flex gap-3">
                <button
                  onClick={() => { setStep('code'); setError(null); }}
                  className="flex-1 border border-gray-200 text-[#2C3E50] rounded-lg
                             py-2.5 text-sm font-medium hover:bg-gray-50 transition-colors"
                >
                  Reintentar
                </button>
                <button
                  onClick={() => navigate('/')}
                  className="flex-1 bg-[#1A8C7A] text-white rounded-lg py-2.5
                             text-sm font-medium hover:bg-[#126B5E] transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default SmartWatchPairingPage;