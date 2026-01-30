import axios from 'axios';

// Helper function to get cookie value by name
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api', // Use env var in prod, proxy in dev
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Required for CSRF cookies to be sent/received
});

// Initialize CSRF token by making a GET request to receive the cookie
// Call this once when the app loads (e.g., in App.jsx or main.jsx)
export async function initializeCsrf() {
  try {
    // Make a simple GET request to a public endpoint to receive XSRF-TOKEN cookie
    await api.get('/ai/daily-quote');
  } catch (e) {
    // Ignore errors - we just want the cookie
    console.debug('CSRF cookie initialization attempted');
  }
}

// Add a request interceptor to attach the JWT token and CSRF token
api.interceptors.request.use(
  (config) => {
    // Attach JWT token from localStorage
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Attach CSRF token from cookie (Double Submit Cookie pattern)
    // The backend sends XSRF-TOKEN cookie, we send it back in X-XSRF-TOKEN header
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
