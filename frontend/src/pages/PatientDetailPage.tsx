import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Input  from '../components/ui/Input';
import BiometricHistory from '../components/biometric/BiometricHistory';
import { patientsApi } from '../api/patients';
import type { PatientDetailResponse, MedicalHistoryResponse } from '../types';
import { ArrowLeft, Save, Trash2, MapPin, Activity, User } from 'lucide-react';

type Tab = 'data' | 'history';

const PatientDetailPage = () => {
  const { id }   = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isNew    = id === 'new';

  const [tab,      setTab]     = useState<Tab>('data');
  const [patient,  setPatient] = useState<PatientDetailResponse | null>(null);
  const [loading,  setLoading] = useState(!isNew);
  const [saving,   setSaving]  = useState(false);
  const [deleting, setDeleting]= useState(false);
  const [error,    setError]   = useState('');
  const [success,  setSuccess] = useState('');

  const [form, setForm] = useState({
    fullName:    '',
    dateOfBirth: '',
  });

  const [history, setHistory] = useState<Partial<MedicalHistoryResponse>>({
    bloodType:             '',
    allergies:             '',
    chronicConditions:     '',
    emergencyInstructions: '',
  });

  useEffect(() => {
    if (isNew || !id) return;
    patientsApi.getById(id)
      .then(({ data }) => {
        setPatient(data);
        setForm({ fullName: data.fullName, dateOfBirth: data.dateOfBirth });
        if (data.medicalHistory) {
          setHistory({
            bloodType:             data.medicalHistory.bloodType ?? '',
            allergies:             data.medicalHistory.allergies ?? '',
            chronicConditions:     data.medicalHistory.chronicConditions ?? '',
            emergencyInstructions: data.medicalHistory.emergencyInstructions ?? '',
          });
        }
      })
      .finally(() => setLoading(false));
  }, [id, isNew]);

  const handleSave = async () => {
    if (!form.fullName || !form.dateOfBirth) {
      setError('El nombre y la fecha de nacimiento son obligatorios');
      return;
    }
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      if (isNew) {
        const { data } = await patientsApi.create(form);
        if (Object.values(history).some(v => v)) {
          await patientsApi.updateMedicalHistory(data.patientId, history);
        }
        navigate(`/patients/${data.patientId}`);
      } else if (id) {
        await patientsApi.update(id, form);
        await patientsApi.updateMedicalHistory(id, history);
        setSuccess('Datos guardados correctamente');
        setTimeout(() => setSuccess(''), 3000);
      }
    } catch {
      setError('Error al guardar los datos');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!id || isNew) return;
    if (!confirm(`¿Eliminar a ${patient?.fullName}? Esta acción no se puede deshacer.`)) return;
    setDeleting(true);
    try {
      await patientsApi.delete(id);
      navigate('/patients');
    } catch {
      setError('Error al eliminar el paciente');
      setDeleting(false);
    }
  };

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
      {/* Cabecera */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/patients')}
            className="p-2 hover:bg-gray-100 rounded-lg transition-all"
          >
            <ArrowLeft size={20} className="text-[#2C3E50]" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-[#2C3E50]">
              {isNew ? 'Nuevo paciente' : (patient?.fullName ?? 'Paciente')}
            </h1>
            {!isNew && patient && (
              <p className="text-sm text-[#7F8C8D]">{patient.age} años</p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2">
          {!isNew && (
            <Button
              variant="secondary"
              onClick={() => navigate(`/safe-zones/${id}`)}
            >
              <MapPin size={16} />
              Zonas seguras
            </Button>
          )}
          {!isNew && (
            <Button variant="danger" loading={deleting} onClick={handleDelete}>
              <Trash2 size={16} />
              Eliminar
            </Button>
          )}
          {tab === 'data' && (
            <Button loading={saving} onClick={handleSave}>
              <Save size={16} />
              {isNew ? 'Crear paciente' : 'Guardar cambios'}
            </Button>
          )}
        </div>
      </div>

      {/* Pestañas */}
      {!isNew && (
        <div className="flex gap-1 mb-6 bg-gray-100 p-1 rounded-xl w-fit">
          <button
            onClick={() => setTab('data')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm
                        font-medium transition-all ${
              tab === 'data'
                ? 'bg-white text-[#2C3E50] shadow-sm'
                : 'text-[#7F8C8D] hover:text-[#2C3E50]'
            }`}
          >
            <User size={14} />
            Datos del paciente
          </button>
          <button
            onClick={() => setTab('history')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm
                        font-medium transition-all ${
              tab === 'history'
                ? 'bg-white text-[#2C3E50] shadow-sm'
                : 'text-[#7F8C8D] hover:text-[#2C3E50]'
            }`}
          >
            <Activity size={14} />
            Historial biométrico
          </button>
        </div>
      )}

      {/* Mensajes */}
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg
                        px-4 py-3 text-sm text-red-700">{error}</div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 rounded-lg
                        px-4 py-3 text-sm text-green-700">{success}</div>
      )}

      {/* Contenido según pestaña */}
      {tab === 'data' ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

          {/* Datos básicos */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h2 className="font-semibold text-[#2C3E50] mb-4">
              Datos personales
            </h2>
            <div className="space-y-4">
              <Input
                label="Nombre completo"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                placeholder="María Antonia García"
                required
              />
              <Input
                label="Fecha de nacimiento"
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                required
              />
            </div>

            {!isNew && patient && (
              <div className="mt-6 pt-6 border-t border-gray-100">
                <h3 className="text-sm font-medium text-[#7F8C8D] mb-3">
                  Dispositivo
                </h3>
                {patient.activeDeviceId ? (
                  <div className="space-y-1 text-sm">
                    <p className="text-[#2C3E50]">
                      <span className="text-[#7F8C8D]">ID: </span>
                      <span className="font-mono text-xs">{patient.activeDeviceId}</span>
                    </p>
                    <p className="text-[#2C3E50]">
                      <span className="text-[#7F8C8D]">Batería: </span>
                      {patient.batteryLevel}%
                    </p>
                    <p className={patient.deviceConnected
                      ? 'text-[#1A8C7A]' : 'text-[#CC2222]'}>
                      {patient.deviceConnected ? '● Conectado' : '● Desconectado'}
                    </p>
                    <button
                      onClick={async () => {
                        if (!confirm('¿Eliminar el smartwatch? Se borrarán alertas y zonas seguras.')) return;
                        await patientsApi.removeDevice(id!);
                        setPatient(prev => prev ? { ...prev, activeDeviceId: undefined } : prev);
                        setSuccess('Smartwatch eliminado correctamente');
                      }}
                      className="mt-2 flex items-center gap-1.5 text-xs text-[#CC2222]
                                hover:text-red-700 font-medium transition-colors"
                    >
                      <Trash2 size={12} />
                      Desvincular smartwatch
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-[#7F8C8D]">Sin dispositivo asignado</p>
                )}
              </div>
            )}
          </div>

          {/* Historial médico */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6">
            <h2 className="font-semibold text-[#2C3E50] mb-4">
              Historial médico
            </h2>
            <div className="space-y-4">
              <Input
                label="Grupo sanguíneo"
                value={history.bloodType ?? ''}
                onChange={(e) => setHistory({ ...history, bloodType: e.target.value })}
                placeholder="A+, O-, B+..."
              />
              {(['allergies', 'chronicConditions', 'emergencyInstructions'] as const).map((field) => (
                <div key={field} className="flex flex-col gap-1">
                  <label className="text-sm font-medium text-gray-700">
                    {{
                      allergies:             'Alergias',
                      chronicConditions:     'Condiciones crónicas',
                      emergencyInstructions: 'Instrucciones de emergencia',
                    }[field]}
                  </label>
                  <textarea
                    value={history[field] ?? ''}
                    onChange={(e) => setHistory({ ...history, [field]: e.target.value })}
                    rows={field === 'emergencyInstructions' ? 3 : 2}
                    className="px-3 py-2 border border-gray-300 rounded-lg outline-none
                               focus:ring-2 focus:ring-[#1A8C7A] focus:border-[#1A8C7A]
                               resize-none text-sm"
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : (
        id && patient?.activeDeviceId && (
          <BiometricHistory
            patientId={id}
            deviceId={patient.activeDeviceId}
          />
        )
      )}
    </Layout>
  );
};

export default PatientDetailPage;