import { motion } from 'framer-motion';
import { ArrowLeft, CheckCircle, Loader2, Mail } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../lib/axios';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await api.post('/auth/forgot-password', { email });
      setSubmitted(true);
    } catch (err) {
      // We show success regardless to prevent user enumeration
      // But if there's a network error, we should show it
      if (err.message === 'Network Error') {
        setError('Unable to connect to server. Please try again later.');
      } else {
        setSubmitted(true);
      }
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
        {/* Background Decor */}
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary-DEFAULT/20 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[30%] h-[30%] bg-[#4A00E0]/20 rounded-full blur-[100px]" />

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="w-full max-w-md bg-dark-card backdrop-blur-2xl border border-white/10 p-8 rounded-3xl shadow-2xl relative z-10 text-center"
        >
          <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="text-green-400" size={32} />
          </div>
          
          <h1 className="text-2xl font-bold text-white mb-4">Check Your Email</h1>
          
          <p className="text-gray-400 mb-6">
            If an account exists with <span className="text-white font-medium">{email}</span>, 
            you will receive a password reset link shortly.
          </p>

          <p className="text-gray-500 text-sm mb-8">
            The link will expire in 30 minutes. If you don't see the email, check your spam folder.
          </p>

          <div className="space-y-4">
            <Link
              to="/login"
              className="block w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity text-center"
            >
              Back to Login
            </Link>
            
            <button
              onClick={() => {
                setSubmitted(false);
                setEmail('');
              }}
              className="w-full text-gray-400 hover:text-white transition-colors py-2"
            >
              Try a different email
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

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
        <Link 
          to="/login" 
          className="inline-flex items-center gap-2 text-gray-400 hover:text-white transition-colors mb-6"
        >
          <ArrowLeft size={18} />
          Back to login
        </Link>

        <div className="mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">Forgot Password?</h1>
          <p className="text-gray-400">
            No worries! Enter your email and we'll send you a reset link.
          </p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-xl mb-6 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">
              Email Address
            </label>
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

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <Loader2 className="animate-spin" size={20} />
                Sending...
              </>
            ) : (
              'Send Reset Link'
            )}
          </button>
        </form>

        <p className="mt-8 text-center text-gray-400 text-sm">
          Remember your password?{' '}
          <Link to="/login" className="text-primary-DEFAULT hover:underline font-medium">
            Sign in
          </Link>
        </p>
      </motion.div>
    </div>
  );
};

export default ForgotPassword;
