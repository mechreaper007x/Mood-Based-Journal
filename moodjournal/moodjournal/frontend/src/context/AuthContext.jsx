import { createContext, useContext, useEffect, useState } from 'react';
import api, { initializeCsrf } from '../lib/axios';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [profileComplete, setProfileComplete] = useState(null); // null = unknown, true/false = checked

  const checkProfileComplete = async (token) => {
    try {
      // Use api instance to ensure withCredentials and CSRF handling
      const response = await api.get('/profile/complete', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setProfileComplete(response.data.isComplete);
      return response.data.isComplete;
    } catch (err) {
      console.error('Failed to check profile:', err);
      // On network errors, assume profile is complete to avoid blocking user
      // Only a successful API response with isComplete:false should trigger onboarding
      setProfileComplete(true);
      return true;
    }
  };

  useEffect(() => {
    // Initialize CSRF token on app load
    initializeCsrf();
    
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
    const response = await api.post('/auth/register', { username, email, password, age });
    const { token, user: userData } = response.data;
    
    // Store token and user data just like login
    localStorage.setItem('token', token);
    const userObj = userData?.email ? userData : { username, email };
    localStorage.setItem('user', JSON.stringify(userObj));
    setUser(userObj);
    
    // Check profile status after registration
    await checkProfileComplete(token);
    
    return userObj;
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
