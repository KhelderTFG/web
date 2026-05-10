import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from  './hooks/useAuth';
import { AuthProvider } from './context/AuthContext';
import SafeZonePage from './pages/SafeZonePage';

// Páginas (las crearemos en issues siguientes)
import LoginPage        from './pages/LoginPage';
import DashboardPage    from './pages/DashboardPage';
import PatientsPage     from './pages/PatientsPage';
import PatientDetailPage from './pages/PatientDetailPage';
import AlertsPage       from './pages/AlertsPage';
import RemindersPage    from './pages/RemindersPage';
import ReportsPage      from './pages/ReportsPage';
import SmartWatchPairingPage from './pages/SmartwatchPairingPage';
import RegisterPage from './pages/RegisterPage';

// Ruta protegida
const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { isLoggedIn } = useAuth();
  return isLoggedIn ? <>{children}</> : <Navigate to="/login" replace />;
};

const AppRoutes = () => (
  <Routes>
    <Route path="/login" element={<LoginPage />
    } />
    <Route path="/" element={
      <PrivateRoute><DashboardPage /></PrivateRoute>
    } />
    <Route path="/patients" element={
      <PrivateRoute><PatientsPage /></PrivateRoute>
    } />
    <Route path="/patients/:id" element={
      <PrivateRoute><PatientDetailPage /></PrivateRoute>
    } />
    <Route path="/alerts" element={
      <PrivateRoute><AlertsPage /></PrivateRoute>
    } />
    <Route path="/reminders" element={
      <PrivateRoute><RemindersPage /></PrivateRoute>
    } />
    <Route path="/reports" element={
      <PrivateRoute><ReportsPage /></PrivateRoute>
    } />
    <Route path="/safe-zones/:patientId" element={
      <PrivateRoute><SafeZonePage /></PrivateRoute>
    } />
    <Route path="*" element={<Navigate to="/" replace />
    } />
    <Route path="/pairing" element={<SmartWatchPairingPage />
    } />
    <Route path="/register" element={<RegisterPage />
    } />
  </Routes>
);

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;