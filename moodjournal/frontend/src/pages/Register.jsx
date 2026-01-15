import { motion } from 'framer-motion';
import { Loader2, Lock, Mail, User } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Register = () => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [age, setAge] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

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
                <div className="relative">
                    <Lock className="absolute left-4 top-3.5 text-gray-500" size={20} />
                    <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} 
                        className="w-full bg-dark-input border border-white/10 rounded-xl py-3 pl-12 text-white focus:border-primary-DEFAULT outline-none" required />
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
