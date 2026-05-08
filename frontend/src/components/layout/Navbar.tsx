import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Watch } from 'lucide-react';

const Navbar = () => {
  const { logout, caregiver } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-lg text-sm font-medium transition-all ${
      isActive
        ? 'bg-[#1A8C7A] text-white'
        : 'text-[#2C3E50] hover:bg-gray-100'
    }`;

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3">
      <div className="max-w-7xl mx-auto flex items-center justify-between">

        {/* Logo */}
        <div className="flex items-center gap-2">
          <img
            src="/logo-khelder-2.png"
            alt="Khelder"
            className="w-16 h-16 object-contain"
          />
          <span className="font-bold text-[#2C3E50]">Khelder</span>
        </div>

        {/* Links */}
        <div className="flex items-center gap-1">
          <NavLink to="/"          end className={linkClass}>Inicio</NavLink>
          <NavLink to="/patients"      className={linkClass}>Pacientes</NavLink>
          <NavLink to="/alerts"        className={linkClass}>Alertas</NavLink>
          <NavLink to="/reminders"     className={linkClass}>Recordatorios</NavLink>
          <NavLink to="/reports"       className={linkClass}>Informes</NavLink>
          <NavLink to="/pairing"       className={linkClass}>
            <span className="flex items-center gap-1.5">
              <Watch size={14} />
              Vincular Watch
            </span>
          </NavLink>
        </div>

        {/* Usuario y logout */}
        <div className="flex items-center gap-3">
          <span className="text-sm text-[#7F8C8D]">{caregiver?.name}</span>
          <button
            onClick={handleLogout}
            className="px-4 py-2 text-sm font-medium text-[#CC2222]
                       hover:bg-red-50 rounded-lg transition-all"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;