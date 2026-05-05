import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import Button from '../components/ui/Button';
import { patientsApi } from '../api/patients';
import type { PatientResponse } from '../types';
import { UserPlus, ChevronRight, Wifi, WifiOff, Battery } from 'lucide-react';

const PatientsPage = () => {
  const navigate = useNavigate();
  const [patients, setPatients] = useState<PatientResponse[]>([]);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    patientsApi.getAll()
      .then(({ data }) => setPatients(data))
      .finally(() => setLoading(false));
  }, []);

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
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[#2C3E50]">Pacientes</h1>
          <p className="text-sm text-[#7F8C8D] mt-1">
            {patients.length} paciente{patients.length !== 1 ? 's' : ''} asignado{patients.length !== 1 ? 's' : ''}
          </p>
        </div>
        <Button onClick={() => navigate('/patients/new')}>
          <UserPlus size={16} />
          Añadir paciente
        </Button>
      </div>

      {patients.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-2xl p-12
                        text-center">
          <p className="text-[#7F8C8D] mb-4">No tienes pacientes asignados</p>
          <Button onClick={() => navigate('/patients/new')}>
            <UserPlus size={16} />
            Añadir primer paciente
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {patients.map((patient) => (
            <PatientCard
              key={patient.patientId}
              patient={patient}
              onClick={() => navigate(`/patients/${patient.patientId}`)}
            />
          ))}
        </div>
      )}
    </Layout>
  );
};

// ---- Tarjeta de paciente ----
interface PatientCardProps {
  patient: PatientResponse;
  onClick: () => void;
}

const PatientCard = ({ patient, onClick }: PatientCardProps) => {
  const batteryColor = (level: number | null) => {
    if (level === null) return 'text-[#7F8C8D]';
    if (level < 15)     return 'text-[#CC2222]';
    if (level < 30)     return 'text-[#E67E22]';
    return 'text-[#1A8C7A]';
  };

  return (
    <button
      onClick={onClick}
      className="bg-white border border-gray-200 rounded-2xl p-5 text-left
                 hover:border-[#1A8C7A] hover:shadow-sm transition-all w-full"
    >
      {/* Cabecera */}
      <div className="flex items-start justify-between mb-4">
        <div>
          <h3 className="font-semibold text-[#2C3E50]">{patient.fullName}</h3>
          <p className="text-xs text-[#7F8C8D] mt-0.5">{patient.age} años</p>
        </div>
        <ChevronRight size={18} className="text-[#7F8C8D] mt-0.5" />
      </div>

      {/* Estado dispositivo */}
      <div className="flex items-center justify-between text-sm">
        <div className="flex items-center gap-1.5">
          {patient.deviceConnected ? (
            <Wifi size={14} className="text-[#1A8C7A]" />
          ) : (
            <WifiOff size={14} className="text-[#CC2222]" />
          )}
          <span className={patient.deviceConnected
            ? 'text-[#1A8C7A]' : 'text-[#CC2222]'}>
            {patient.deviceConnected ? 'Conectado' : 'Desconectado'}
          </span>
        </div>

        {patient.batteryLevel !== null && (
          <div className={`flex items-center gap-1 ${batteryColor(patient.batteryLevel)}`}>
            <Battery size={14} />
            <span className="text-xs">{patient.batteryLevel}%</span>
          </div>
        )}
      </div>

      {/* Alertas activas */}
      {patient.activeAlertsCount > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-100">
          <span className="inline-flex items-center gap-1 text-xs font-medium
                           text-[#CC2222] bg-red-50 px-2 py-1 rounded-full">
            ⚠️ {patient.activeAlertsCount} alerta{patient.activeAlertsCount !== 1 ? 's' : ''} activa{patient.activeAlertsCount !== 1 ? 's' : ''}
          </span>
        </div>
      )}
    </button>
  );
};

export default PatientsPage;