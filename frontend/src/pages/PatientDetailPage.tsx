import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Input  from '../components/ui/Input';
import { patientsApi } from '../api/patients';
import type { PatientDetailResponse, MedicalHistoryResponse } from '../types';
import { ArrowLeft, Save, Trash2, MapPin } from 'lucide-react';

const PatientDetailPage = () => {
  const { id }     = useParams<{ id: string }>();
  const navigate   = useNavigate();
  const isNew      = id === 'new';

  const [patient,  setPatient]  = useState<PatientDetailResponse | null>(null);
  const [loading,  setLoading]  = useState(!isNew);
  const [saving,   setSaving]   = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error,    setError]    = useState('');
  const [success,  setSuccess]  = useState('');

  // Formulario datos básicos
  const [form, setForm] = useState({
    fullName:    '',
    dateOfBirth: '',
  });

  // Formulario historial médico
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
        setForm({
          fullName:    data.fullName,
          dateOfBirth: data.dateOfBirth,
        });
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
        // Guardar historial médico si hay datos
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
            <Button
              variant="danger"
              loading={deleting}
              onClick={handleDelete}
            >
              <Trash2 size={16} />
              Eliminar
            </Button>
          )}
          <Button loading={saving} onClick={handleSave}>
            <Save size={16} />
            {isNew ? 'Crear paciente' : 'Guardar cambios'}
          </Button>
        </div>
      </div>

      {/* Mensajes */}
      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 rounded-lg
                        px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 bg-green-50 border border-green-200 rounded-lg
                        px-4 py-3 text-sm text-green-700">
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Datos básicos */}
        <div className="bg-white border border-gray-200 rounded-2xl p-6">
          <h2 className="font-semibold text-[#2C3E50] mb-4">Datos personales</h2>
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

          {/* Info del dispositivo */}
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
                </div>
              ) : (
                <p className="text-sm text-[#7F8C8D]">Sin dispositivo asignado</p>
              )}
            </div>
          )}
        </div>

        {/* Historial médico */}
        <div className="bg-white border border-gray-200 rounded-2xl p-6">
          <h2 className="font-semibold text-[#2C3E50] mb-4">Historial médico</h2>
          <div className="space-y-4">
            <Input
              label="Grupo sanguíneo"
              value={history.bloodType ?? ''}
              onChange={(e) => setHistory({ ...history, bloodType: e.target.value })}
              placeholder="A+, O-, B+..."
            />

            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">
                Alergias
              </label>
              <textarea
                value={history.allergies ?? ''}
                onChange={(e) => setHistory({ ...history, allergies: e.target.value })}
                placeholder="Penicilina, Ibuprofeno..."
                rows={2}
                className="px-3 py-2 border border-gray-300 rounded-lg outline-none
                           focus:ring-2 focus:ring-[#1A8C7A] focus:border-[#1A8C7A]
                           resize-none text-sm"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">
                Condiciones crónicas
              </label>
              <textarea
                value={history.chronicConditions ?? ''}
                onChange={(e) => setHistory({ ...history, chronicConditions: e.target.value })}
                placeholder="Hipertensión, Diabetes tipo 2..."
                rows={2}
                className="px-3 py-2 border border-gray-300 rounded-lg outline-none
                           focus:ring-2 focus:ring-[#1A8C7A] focus:border-[#1A8C7A]
                           resize-none text-sm"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">
                Instrucciones de emergencia
              </label>
              <textarea
                value={history.emergencyInstructions ?? ''}
                onChange={(e) => setHistory({
                  ...history, emergencyInstructions: e.target.value
                })}
                placeholder="Contactar con..."
                rows={3}
                className="px-3 py-2 border border-gray-300 rounded-lg outline-none
                           focus:ring-2 focus:ring-[#1A8C7A] focus:border-[#1A8C7A]
                           resize-none text-sm"
              />
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default PatientDetailPage;