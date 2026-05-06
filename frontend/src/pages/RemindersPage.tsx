import { useEffect, useState, useCallback } from 'react';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import Input  from '../components/ui/Input';
import { remindersApi } from '../api/reminders';
import { patientsApi  } from '../api/patients';
import type { ReminderResponse, ReminderRequest, PatientResponse, ReminderStatus } from '../types';
import { Plus, Trash2, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';

const STATUS_CONFIG: Record<ReminderStatus, {
  label: string; color: string; icon: React.ReactNode
}> = {
  PENDING:       {
    label: 'Pendiente',
    color: 'text-[#E67E22] bg-orange-50 border-orange-200',
    icon:  <Clock size={12} />,
  },
  SENT_TO_WATCH: {
    label: 'Enviado',
    color: 'text-[#1A8C7A] bg-teal-50 border-teal-200',
    icon:  <AlertCircle size={12} />,
  },
  CONFIRMED:     {
    label: 'Confirmado',
    color: 'text-[#1A8C7A] bg-green-50 border-green-200',
    icon:  <CheckCircle size={12} />,
  },
  MISSED:        {
    label: 'Perdido',
    color: 'text-[#CC2222] bg-red-50 border-red-200',
    icon:  <XCircle size={12} />,
  },
};

const RemindersPage = () => {
  const [patients,   setPatients]   = useState<PatientResponse[]>([]);
  const [reminders,  setReminders]  = useState<ReminderResponse[]>([]);
  const [loading,    setLoading]    = useState(true);
  const [selected,   setSelected]   = useState<string>('');
  const [showForm,   setShowForm]   = useState(false);

  // Formulario
  const [form, setForm] = useState<ReminderRequest>({
    patientId:    '',
    message:      '',
    scheduledDate: '',
  });
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');

  // Cargar pacientes
  useEffect(() => {
    patientsApi.getAll().then(({ data }) => {
      setPatients(data);
      if (data.length > 0) {
        setSelected(data[0].patientId);
        setForm(f => ({ ...f, patientId: data[0].patientId }));
      }
    });
  }, []);

  // Cargar recordatorios al cambiar de paciente
  const fetchReminders = useCallback((patientId: string) => {
    if (!patientId) return;
    setLoading(true);
    remindersApi.getByPatient(patientId)
      .then(({ data }) => setReminders(data))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchReminders(selected);
  }, [selected, fetchReminders]);

  const handlePatientChange = (patientId: string) => {
    setSelected(patientId);
    setForm(f => ({ ...f, patientId }));
    setShowForm(false);
    setError('');
  };

  const handleSave = async () => {
    if (!form.message || !form.scheduledDate) {
      setError('El mensaje y la fecha son obligatorios');
      return;
    }

    setSaving(true);
    setError('');

    try {
      await remindersApi.create(form);
      setShowForm(false);
      setForm(f => ({
        ...f,
        message:       '',
        scheduledDate: '',
      }));
      fetchReminders(selected);
    } catch {
      setError('Error al crear el recordatorio');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (reminderId: string) => {
    if (!confirm('¿Eliminar este recordatorio?')) return;
    try {
      await remindersApi.delete(reminderId);
      setReminders(prev => prev.filter(r => r.reminderId !== reminderId));
    } catch {
      // silencioso
    }
  };

  // Agrupar por estado
  const pending   = reminders.filter(r => r.status === 'PENDING');
  const sent      = reminders.filter(r => r.status === 'SENT_TO_WATCH');
  const confirmed = reminders.filter(r => r.status === 'CONFIRMED');
  const missed    = reminders.filter(r => r.status === 'MISSED');

  const selectedPatient = patients.find(p => p.patientId === selected);

  return (
    <Layout>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[#2C3E50]">Recordatorios</h1>
          <p className="text-sm text-[#7F8C8D] mt-1">
            Gestión de recordatorios médicos
          </p>
        </div>
        <Button onClick={() => { setShowForm(true); setError(''); }}>
          <Plus size={16} />
          Nuevo recordatorio
        </Button>
      </div>

      {/* Selector de paciente */}
      {patients.length > 1 && (
        <div className="flex gap-2 mb-6 flex-wrap">
          {patients.map((p) => (
            <button
              key={p.patientId}
              onClick={() => handlePatientChange(p.patientId)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
                ${selected === p.patientId
                  ? 'bg-[#1A8C7A] text-white'
                  : 'bg-white border border-gray-200 text-[#2C3E50] hover:bg-gray-50'
                }`}
            >
              {p.fullName}
            </button>
          ))}
        </div>
      )}

      {/* Formulario nuevo recordatorio */}
      {showForm && (
        <div className="bg-white border border-[#1A8C7A] rounded-2xl p-6 mb-6">
          <h2 className="font-semibold text-[#2C3E50] mb-4">
            Nuevo recordatorio para {selectedPatient?.fullName}
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="text-sm font-medium text-gray-700 block mb-1">
                Mensaje
              </label>
              <textarea
                value={form.message}
                onChange={(e) => setForm({ ...form, message: e.target.value })}
                placeholder="Tomar Enalapril 10mg con el desayuno..."
                rows={2}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg
                           outline-none focus:ring-2 focus:ring-[#1A8C7A]
                           focus:border-[#1A8C7A] resize-none text-sm"
              />
            </div>
            <Input
              label="Fecha y hora"
              type="datetime-local"
              value={form.scheduledDate}
              onChange={(e) => setForm({ ...form, scheduledDate: e.target.value })}
              min={new Date().toISOString().slice(0, 16)}
            />
          </div>

          {error && (
            <p className="text-sm text-red-600 mt-3">{error}</p>
          )}

          <div className="flex gap-2 mt-4">
            <Button loading={saving} onClick={handleSave}>
              Guardar recordatorio
            </Button>
            <Button
              variant="secondary"
              onClick={() => { setShowForm(false); setError(''); }}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}

      {/* Lista de recordatorios */}
      {loading ? (
        <div className="flex items-center justify-center h-48 text-[#7F8C8D]">
          Cargando recordatorios...
        </div>
      ) : reminders.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-2xl p-12
                        text-center">
          <Clock size={40} className="text-gray-300 mx-auto mb-3" />
          <p className="text-[#7F8C8D] mb-4">
            No hay recordatorios para {selectedPatient?.fullName}
          </p>
          <Button onClick={() => setShowForm(true)}>
            <Plus size={16} />
            Crear primer recordatorio
          </Button>
        </div>
      ) : (
        <div className="space-y-6">
          {[
            { title: 'Pendientes',  items: pending,   show: pending.length > 0   },
            { title: 'Enviados',    items: sent,      show: sent.length > 0      },
            { title: 'Confirmados', items: confirmed, show: confirmed.length > 0 },
            { title: 'Perdidos',    items: missed,    show: missed.length > 0    },
          ].filter(g => g.show).map((group) => (
            <div key={group.title}>
              <h3 className="text-sm font-semibold text-[#7F8C8D] uppercase
                             tracking-wide mb-3">
                {group.title} ({group.items.length})
              </h3>
              <div className="space-y-3">
                {group.items.map((reminder) => (
                  <ReminderCard
                    key={reminder.reminderId}
                    reminder={reminder}
                    onDelete={handleDelete}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </Layout>
  );
};

// ---- Tarjeta de recordatorio ----
interface ReminderCardProps {
  reminder: ReminderResponse;
  onDelete: (id: string) => void;
}

const ReminderCard = ({ reminder, onDelete }: ReminderCardProps) => {
  const config = STATUS_CONFIG[reminder.status];
  const date   = new Date(reminder.scheduledDate);
  const isPast = date < new Date();

  return (
    <div className={`bg-white border rounded-2xl p-5 flex items-start
                     justify-between gap-4
                     ${isPast && reminder.status === 'PENDING'
                       ? 'border-orange-300'
                       : 'border-gray-200'}`}>
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-2 flex-wrap">
          <span className={`inline-flex items-center gap-1 text-xs font-medium
                            px-2 py-0.5 rounded-full border ${config.color}`}>
            {config.icon}
            {config.label}
          </span>
          {isPast && reminder.status === 'PENDING' && (
            <span className="text-xs text-[#E67E22]">⚠️ Vencido</span>
          )}
        </div>

        <p className="text-sm font-medium text-[#2C3E50] mb-1">
          {reminder.message}
        </p>

        <p className="text-xs text-[#7F8C8D]">
          {format(date, "EEEE d 'de' MMMM 'a las' HH:mm", { locale: es })}
        </p>
      </div>

      {reminder.status === 'PENDING' && (
        <button
          onClick={() => onDelete(reminder.reminderId)}
          className="p-2 text-[#7F8C8D] hover:text-[#CC2222]
                     hover:bg-red-50 rounded-lg transition-all"
        >
          <Trash2 size={16} />
        </button>
      )}
    </div>
  );
};

export default RemindersPage;