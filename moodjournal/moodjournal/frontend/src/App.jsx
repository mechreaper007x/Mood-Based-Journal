import { Loader2 } from 'lucide-react';
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import DisclaimerModal from './components/DisclaimerModal';
import Layout from './components/Layout';
import { AuthProvider, useAuth } from './context/AuthContext';
import Analytics from './pages/Analytics';
import Dashboard from './pages/Dashboard';
import DeepAssessment from './pages/DeepAssessment';
import ForgotPassword from './pages/ForgotPassword';
import Goals from './pages/Goals';
import History from './pages/History';
import JournalEntry from './pages/JournalEntry';
import Login from './pages/Login';
import Onboarding from './pages/Onboarding';
import Profile from './pages/Profile';
import Register from './pages/Register';
import ResetPassword from './pages/ResetPassword';
import ViewEntry from './pages/ViewEntry';


const ProtectedRoute = () => {
  const { user, loading, profileComplete } = useAuth();
  
  if (loading) return <div className="h-screen w-full flex items-center justify-center bg-dark-bg text-primary-DEFAULT"><Loader2 className="animate-spin" size={40}/></div>;
  
  if (!user) return <Navigate to="/login" replace />;
  
  
  if (profileComplete === false) return <Navigate to="/onboarding" replace />;
  
  return <Layout><Outlet /></Layout>;
};


const OnboardingRoute = () => {
  const { user, loading, profileComplete } = useAuth();
  
  if (loading) return <div className="h-screen w-full flex items-center justify-center bg-dark-bg text-primary-DEFAULT"><Loader2 className="animate-spin" size={40}/></div>;
  
  if (!user) return <Navigate to="/login" replace />;
  
  
  if (profileComplete === true) return <Navigate to="/dashboard" replace />;
  
  return <Onboarding />;
};

function App() {
  return (
    <AuthProvider>
      <DisclaimerModal />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          
          <Route path="/onboarding" element={<OnboardingRoute />} />
          
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/entry" element={<JournalEntry />} />
            <Route path="/entry/:id" element={<ViewEntry />} />
            <Route path="/journal" element={<History />} />
            <Route path="/assessment" element={<DeepAssessment />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/goals" element={<Goals />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

