import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { authApi } from '../api/auth';
import Button from '../components/ui/Button';
import Input  from '../components/ui/Input';

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const { data } = await authApi.login(form);
      login(data);
      navigate('/');
    } catch {
      setError('Email o contraseña incorrectos');
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

        {/* Tarjeta de login */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
          <h2 className="text-xl font-semibold text-[#2C3E50] mb-6">
            Iniciar sesión
          </h2>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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
              label="Contraseña"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="••••••••"
              required
              autoComplete="current-password"
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
              Entrar
            </Button>
          </form>
        </div>

        {/* Credenciales de prueba */}
        <div className="mt-4 bg-[#1A8C7A]/10 border border-[#1A8C7A]/20
                        rounded-xl p-4 text-sm text-[#126B5E]">
          <p className="font-medium mb-1">Credenciales de prueba:</p>
          <p>carlos@khelder.com / password123</p>
          <p>ana@khelder.com / password123</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;