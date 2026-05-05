import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

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
          <div className="w-8 h-8 bg-[#1A8C7A] rounded-lg flex items-center
                          justify-center">
            <svg viewBox="0 0 24 24" fill="none" className="w-5 h-5 text-white"
                 stroke="currentColor" strokeWidth="2.5">
              <path strokeLinecap="round" strokeLinejoin="round"
                d="M4.5 12.5l2 2 3-4 3 4 3-4 2 2" />
            </svg>
          </div>
          <span className="font-bold text-[#2C3E50]">Khelder</span>
        </div>

        {/* Links */}
        <div className="flex items-center gap-1">
          <NavLink to="/"        end className={linkClass}>Inicio</NavLink>
          <NavLink to="/patients"    className={linkClass}>Pacientes</NavLink>
          <NavLink to="/alerts"      className={linkClass}>Alertas</NavLink>
          <NavLink to="/reminders"   className={linkClass}>Recordatorios</NavLink>
          <NavLink to="/reports"     className={linkClass}>Informes</NavLink>
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