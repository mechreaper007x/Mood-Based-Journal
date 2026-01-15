import { motion } from 'framer-motion';
import { Loader2, Lock, Mail } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, profileComplete } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      // AuthContext now has profileComplete status, redirect accordingly
      // The ProtectedRoute will also handle this, but this provides immediate feedback
      navigate('/dashboard');
    } catch (err) {
      setError('Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
      {/* Background Decor */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary-DEFAULT/20 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[30%] h-[30%] bg-[#4A00E0]/20 rounded-full blur-[100px]" />

      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md bg-dark-card backdrop-blur-2xl border border-white/10 p-8 rounded-3xl shadow-2xl relative z-10"
      >
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Welcome Back</h1>
          <p className="text-gray-400">Sign in to continue your journey</p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-xl mb-6 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">Email</label>
            <div className="relative">
              <Mail className="absolute left-4 top-3.5 text-gray-500" size={20} />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white focus:outline-none focus:border-primary-DEFAULT transition-colors"
                placeholder="you@example.com"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">Password</label>
            <div className="relative">
              <Lock className="absolute left-4 top-3.5 text-gray-500" size={20} />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white focus:outline-none focus:border-primary-DEFAULT transition-colors"
                placeholder="••••••••"
                required
              />
            </div>
            <div className="mt-2 text-right">
              <Link to="/forgot-password" className="text-sm text-primary-DEFAULT hover:underline">
                Forgot Password?
              </Link>
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
          >
            {loading ? <Loader2 className="animate-spin" size={20} /> : 'Sign In'}
          </button>
        </form>

        <p className="mt-8 text-center text-gray-400 text-sm">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary-DEFAULT hover:underline font-medium">
            Create one
          </Link>
        </p>
      </motion.div>
    </div>
  );
};

export default Login;
