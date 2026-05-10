import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { authApi } from '../api/auth';
import Button from '../components/ui/Button';
import Input  from '../components/ui/Input';

const RegisterPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({
    name:     '',
    email:    '',
    password: '',
    phone:    '',
  });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (form.password !== confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    if (form.password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres');
      return;
    }

    setLoading(true);
    try {
      const { data } = await authApi.register(form);
      login(data);
      navigate('/');
    } catch (err: any) {
      const msg = err?.response?.data?.message;
      setError(msg ?? 'Error al crear la cuenta. Inténtalo de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">

        {/* Logo y título */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-40 h-40
                          rounded-2xl bg-white shadow-sm mb-4 p-2">
            <img
              src="/logo-khelder-2.png"
              alt="Khelder"
              className="w-full h-full object-contain"
            />
          </div>
          <h1 className="text-3xl font-bold text-[#2C3E50]">Khelder</h1>
          <p className="text-[#7F8C8D] mt-1">
            Panel de monitorización para cuidadores
          </p>
        </div>

        {/* Tarjeta de registro */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <h2 className="text-xl font-semibold text-[#2C3E50] mb-6">
            Crear cuenta
          </h2>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="Nombre completo"
              type="text"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Carlos Martínez"
              required
              autoComplete="name"
            />

            <Input
              label="Correo electrónico"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="cuidador@ejemplo.com"
              required
              autoComplete="email"
            />

            <Input
              label="Teléfono"
              type="tel"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              placeholder="600 123 456"
              autoComplete="tel"
            />

            <Input
              label="Contraseña"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="Mínimo 8 caracteres"
              required
              autoComplete="new-password"
            />

            <Input
              label="Confirmar contraseña"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••"
              required
              autoComplete="new-password"
            />

            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg
                              px-4 py-3 text-sm text-red-700">
                {error}
              </div>
            )}

            <Button
              type="submit"
              loading={loading}
              className="w-full mt-2 py-3"
            >
              Crear cuenta
            </Button>
          </form>

          <p className="text-center text-sm text-[#7F8C8D] mt-6">
            ¿Ya tienes cuenta?{' '}
            <Link
              to="/login"
              className="text-[#1A8C7A] font-medium hover:text-[#126B5E]
                         transition-colors"
            >
              Iniciar sesión
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;