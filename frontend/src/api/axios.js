// src/api/axios.js
import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
});

// 1. Интерцептор запросов — добавляет JWT токен
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 2. Интерцептор ответов — обрабатывает истёкший токен
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');

            const { pathname } = window.location;
            if (pathname !== '/auth' && pathname !== '/register') {
                window.location.href = '/auth';
            }
        }
        return Promise.reject(error);
    }
);

export default api;
