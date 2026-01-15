require('whatwg-fetch');
const { TextEncoder, TextDecoder } = require('util');

global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

const localStorageMock = {
    store: {},
    getItem: jest.fn(key => localStorageMock.store[key] || null),
    setItem: jest.fn((key, value) => {
        localStorageMock.store[key] = String(value);
    }),
    removeItem: jest.fn(key => {
        delete localStorageMock.store[key];
    }),
    clear: jest.fn(() => {
        localStorageMock.store = {};
    })
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });
delete window.location;
window.location = {
    href: '',
    origin: 'http://localhost'
};
// This console.log has been removed as it is not necessary for test validation.

// Mock fetch
global.fetch = jest.fn((url, options) => {
    if (url === 'http://localhost:9092/api/auth/register') {
        const body = JSON.parse(options.body);
        if (body.email === 'existing@example.com' || body.username === 'existinguser') {
            return Promise.resolve({
                ok: false,
                status: 409,
                headers: { get: () => 'application/json' },
                text: () => Promise.resolve('Email or username already taken!'),
                json: () => Promise.resolve({ error: 'Email or username already taken!' }) // For cases where json() might be called
            });
        }
        return Promise.resolve({
            ok: true,
            status: 200,
            headers: { get: () => 'application/json' },
            json: () => Promise.resolve({ token: 'mock-token', user: { id: '123' } })
        });
    }
    if (url === 'http://localhost:9092/api/auth/login') {
        const body = JSON.parse(options.body);
        if (body.email === 'test@example.com' && body.password === 'password') {
            return Promise.resolve({
                ok: true,
                status: 200,
                headers: { get: () => 'application/json' },
                json: () => Promise.resolve({ token: 'mock-token', user: { id: '123' } })
            });
        }
        return Promise.resolve({
            ok: false,
            status: 401,
            headers: { get: () => 'application/json' },
            json: () => Promise.resolve({ error: 'Invalid credentials' })
        });
    }
    return Promise.reject(new Error('not found'));
});