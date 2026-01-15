import { motion } from 'framer-motion';
import { ArrowLeft, CheckCircle, Eye, EyeOff, Loader2, Lock, XCircle } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../lib/axios';

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  // Password strength calculation
  const getPasswordStrength = (pwd) => {
    let strength = 0;
    if (pwd.length >= 8) strength++;
    if (pwd.length >= 12) strength++;
    if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) strength++;
    if (/\d/.test(pwd)) strength++;
    if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) strength++;
    return strength;
  };

  const passwordStrength = getPasswordStrength(password);
  const strengthLabels = ['Very Weak', 'Weak', 'Fair', 'Good', 'Strong'];
  const strengthColors = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-lime-500', 'bg-green-500'];

  // Validate token on mount
  useEffect(() => {
    const validateToken = async () => {
      if (!token) {
        setValidating(false);
        setError('No reset token provided. Please request a new password reset.');
        return;
      }

      try {
        const response = await api.get(`/auth/validate-reset-token?token=${token}`);
        if (response.data.valid) {
          setTokenValid(true);
          setEmail(response.data.email);
        } else {
          setError(response.data.error || 'Invalid or expired token.');
        }
      } catch (err) {
        setError(err.response?.data?.error || 'Invalid or expired token. Please request a new password reset.');
      } finally {
        setValidating(false);
      }
    };

    validateToken();
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    setLoading(true);

    try {
      await api.post('/auth/reset-password', {
        token,
        newPassword: password
      });
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to reset password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Loading state while validating token
  if (validating) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-dark-bg">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center"
        >
          <Loader2 className="animate-spin text-primary-DEFAULT mx-auto mb-4" size={48} />
          <p className="text-gray-400">Validating reset link...</p>
        </motion.div>
      </div>
    );
  }

  // Invalid token state
  if (!tokenValid && !success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-red-500/10 rounded-full blur-[120px]" />
        
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="w-full max-w-md bg-dark-card backdrop-blur-2xl border border-white/10 p-8 rounded-3xl shadow-2xl relative z-10 text-center"
        >
          <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
            <XCircle className="text-red-400" size={32} />
          </div>
          
          <h1 className="text-2xl font-bold text-white mb-4">Invalid Reset Link</h1>
          
          <p className="text-gray-400 mb-8">{error}</p>

          <Link
            to="/forgot-password"
            className="block w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity text-center"
          >
            Request New Reset Link
          </Link>
        </motion.div>
      </div>
    );
  }

  // Success state
  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-green-500/10 rounded-full blur-[120px]" />
        
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="w-full max-w-md bg-dark-card backdrop-blur-2xl border border-white/10 p-8 rounded-3xl shadow-2xl relative z-10 text-center"
        >
          <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
            <CheckCircle className="text-green-400" size={32} />
          </div>
          
          <h1 className="text-2xl font-bold text-white mb-4">Password Reset Successful!</h1>
          
          <p className="text-gray-400 mb-8">
            Your password has been updated. You can now log in with your new password.
          </p>

          <button
            onClick={() => navigate('/login')}
            className="w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity"
          >
            Go to Login
          </button>
        </motion.div>
      </div>
    );
  }

  // Reset password form
  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
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
          <h1 className="text-3xl font-bold text-white mb-2">Reset Password</h1>
          <p className="text-gray-400">
            Create a new password for <span className="text-white">{email}</span>
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
              New Password
            </label>
            <div className="relative">
              <Lock className="absolute left-4 top-3.5 text-gray-500" size={20} />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 pr-12 text-white focus:outline-none focus:border-primary-DEFAULT transition-colors"
                placeholder="••••••••"
                required
                minLength={8}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-3.5 text-gray-500 hover:text-gray-300"
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            
            {/* Password Strength Indicator */}
            {password && (
              <div className="mt-3">
                <div className="flex gap-1 mb-1">
                  {[0, 1, 2, 3, 4].map((i) => (
                    <div
                      key={i}
                      className={`h-1.5 flex-1 rounded-full transition-colors ${
                        i < passwordStrength ? strengthColors[passwordStrength - 1] : 'bg-gray-700'
                      }`}
                    />
                  ))}
                </div>
                <p className={`text-xs ${
                  passwordStrength < 2 ? 'text-red-400' : 
                  passwordStrength < 4 ? 'text-yellow-400' : 'text-green-400'
                }`}>
                  {strengthLabels[passwordStrength - 1] || 'Very Weak'}
                </p>
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">
              Confirm Password
            </label>
            <div className="relative">
              <Lock className="absolute left-4 top-3.5 text-gray-500" size={20} />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={`w-full bg-dark-input border rounded-xl py-3 pl-12 pr-12 text-white focus:outline-none transition-colors ${
                  confirmPassword && confirmPassword !== password 
                    ? 'border-red-500/50 focus:border-red-500' 
                    : 'border-white/10 focus:border-primary-DEFAULT'
                }`}
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-3.5 text-gray-500 hover:text-gray-300"
              >
                {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            {confirmPassword && confirmPassword !== password && (
              <p className="text-red-400 text-xs mt-2">Passwords do not match</p>
            )}
          </div>

          <button
            type="submit"
            disabled={loading || password !== confirmPassword || password.length < 8}
            className="w-full bg-primary-gradient text-white font-semibold py-3.5 rounded-xl hover:opacity-90 transition-opacity flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? (
              <>
                <Loader2 className="animate-spin" size={20} />
                Resetting...
              </>
            ) : (
              'Reset Password'
            )}
          </button>
        </form>
      </motion.div>
    </div>
  );
};

export default ResetPassword;
