import { motion } from 'framer-motion';
import { Eye, EyeOff, Loader2, Lock, Mail, User } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [age, setAge] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(username, email, password, age);
      navigate('/login'); 
    } catch (err) {
      // Extract specific error message from backend if available
      const msg = err.response?.data?.error || 'Registration failed. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg p-4 relative overflow-hidden">
        <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-primary-DEFAULT/20 rounded-full blur-[120px]" />
        
        <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="w-full max-w-md bg-dark-card backdrop-blur-2xl border border-white/10 p-8 rounded-3xl shadow-2xl relative z-10"
        >
            <h1 className="text-3xl font-bold text-white text-center mb-8">Create Account</h1>

            {error && <div className="text-red-400 text-center mb-4">{error}</div>}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="relative">
                    <User className="absolute left-4 top-3.5 text-gray-500" size={20} />
                    <input type="text" placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} 
                        className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 text-white focus:border-primary-DEFAULT outline-none" required />
                </div>
                <div className="relative">
                    <Mail className="absolute left-4 top-3.5 text-gray-500" size={20} />
                    <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} 
                        className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 text-white focus:border-primary-DEFAULT outline-none" required />
                </div>
                <div>
                    <div className="relative">
                        <Lock className="absolute left-4 top-3.5 text-gray-500" size={20} />
                        <input 
                            type={showPassword ? 'text' : 'password'} 
                            placeholder="Password" 
                            value={password} 
                            onChange={e => setPassword(e.target.value)} 
                            className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 pr-12 text-white focus:border-primary-DEFAULT outline-none" 
                            required 
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-4 top-3.5 text-gray-500 hover:text-gray-300 transition-colors"
                        >
                            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                        </button>
                    </div>
                    {/* Password Strength Indicator */}
                    {password && (
                        <div className="mt-2">
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
                <div className="relative">
                    <input type="number" placeholder="Age (Optional)" value={age} onChange={e => setAge(e.target.value)} 
                        className="w-full bg-dark-input border border-white/10 rounded-xl py-3 px-4 text-white focus:border-primary-DEFAULT outline-none" />
                </div>

                <button disabled={loading} className="w-full bg-primary-gradient text-white py-3.5 rounded-xl font-bold hover:opacity-90 flex justify-center">
                    {loading ? <Loader2 className="animate-spin" /> : 'Sign Up'}
                </button>
            </form>
             <p className="mt-8 text-center text-gray-400 text-sm">
                Already have an account? <Link to="/login" className="text-primary-DEFAULT hover:underline">Log in</Link>
            </p>
        </motion.div>
    </div>
  );
};
export default Register;
