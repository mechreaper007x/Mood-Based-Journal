import { createContext, useContext, useEffect, useState } from 'react';
import api from '../lib/axios';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:9092';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [profileComplete, setProfileComplete] = useState(null); // null = unknown, true/false = checked

  const checkProfileComplete = async (token) => {
    try {
      const response = await fetch(`${API_URL}/api/profile/complete`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setProfileComplete(data.isComplete);
        return data.isComplete;
      }
      setProfileComplete(false);
      return false;
    } catch (err) {
      console.error('Failed to check profile:', err);
      setProfileComplete(false);
      return false;
    }
  };

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      const savedUser = localStorage.getItem('user');
      if (savedUser) {
        setUser(JSON.parse(savedUser));
        checkProfileComplete(token);
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    const { token, ...userData } = response.data;
    const jwt = token || response.data.jwt; 
    
    localStorage.setItem('token', jwt);
    const userObj = userData.email ? userData : { email }; 
    localStorage.setItem('user', JSON.stringify(userObj));
    setUser(userObj);
    
    // Check profile status after login
    await checkProfileComplete(jwt);
    
    return userObj;
  };

  const register = async (username, email, password, age) => {
    await api.post('/auth/register', { username, email, password, age });
    return true;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setProfileComplete(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout, loading, profileComplete, checkProfileComplete }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};
