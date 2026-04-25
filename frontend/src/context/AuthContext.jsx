// src/context/AuthContext.jsx
import { createContext, useContext, useState } from 'react';
import api from '../api/axios';

const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const storedUser = localStorage.getItem('user');
        return storedUser ? JSON.parse(storedUser) : null;
    });

    // Применить AuthResponse и записать в state
    const applyAuthResponse = (data) => {
        const { token, id, role, nickname, clubId } = data;
        const userData = {
            id,
            nickname,
            role,
            isAdmin: role.toLowerCase() === 'admin',
            clubId: clubId ?? null,
        };
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
    };

    // updateUserData теперь правильно маппит clubId из UserDto
    // UserDto возвращает { id, nickname, role, clubId, ... }
    const updateUserData = (data) => {
        const { id, role, nickname, clubId } = data;
        const userData = {
            id,
            nickname,
            role,
            isAdmin: role?.toLowerCase() === 'admin',
            // clubId берём напрямую из ответа — null если человек покинул клуб
            clubId: clubId ?? null,
        };
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
        return userData;
    };

    // refreshUser перезапрашивает актуальные данные пользователя
    // Это нужно вызывать после: выхода из клуба, вступления в клуб и т.д.
    const refreshUser = async () => {
        if (!user?.id) return null;
        try {
            const response = await api.get(`/users/${user.id}`);
            return updateUserData(response.data);
        } catch (error) {
            console.error('Не удалось обновить данные пользователя', error);
            if (error.response?.status === 401) {
                logout();
            }
            return null;
        }
    };

    // ── Вход ──────────────────────────────────────────────────────────────────
    const login = async (nickname, password) => {
        try {
            const response = await api.post('/auth/login', { nickname, password });
            applyAuthResponse(response.data);
            return { success: true };
        } catch (error) {
            const status = error.response?.status;
            const data   = error.response?.data;

            if (status === 403 && data?.userId) {
                return {
                    success:         false,
                    emailNotVerified: true,
                    userId:          data.userId,
                    email:           data.email,
                    error:           data.message || 'Email не подтверждён',
                };
            }

            return {
                success: false,
                error:   data?.message || 'Ошибка авторизации. Проверьте данные.',
            };
        }
    };

    // ── Регистрация ───────────────────────────────────────────────────────────
    const register = async (userData) => {
        try {
            const response = await api.post('/auth/register', userData);
            const { userId, email } = response.data;
            return { success: true, userId, email };
        } catch (error) {
            return {
                success: false,
                error: error.response?.data?.message || 'Ошибка регистрации. Возможно, никнейм уже занят.',
            };
        }
    };

    // ── Подтверждение email ───────────────────────────────────────────────────
    const verifyEmail = async (userId, code) => {
        try {
            const response = await api.post('/auth/verify-email', { userId, code });
            applyAuthResponse(response.data);
            return { success: true };
        } catch (error) {
            const status = error.response?.status;
            const message = error.response?.data?.message || 'Неверный или истёкший код.';
            const phantom = status === 403 && message.toLowerCase().includes('фантом');
            return { success: false, error: message, phantom };
        }
    };

    // ── Повторная отправка кода ───────────────────────────────────────────────
    const resendCode = async (userId) => {
        try {
            await api.post('/auth/resend-code', { userId });
            return { success: true };
        } catch (error) {
            const status = error.response?.status;
            const message = error.response?.data?.message || 'Не удалось отправить код.';
            const phantom = status === 403 && message.toLowerCase().includes('фантом');
            return { success: false, error: message, phantom };
        }
    };

    // ── Сброс пароля: запрос письма ───────────────────────────────────────────
    const forgotPassword = async (email) => {
        try {
            await api.post('/auth/forgot-password', { email });
            return { success: true };
        } catch (error) {
            return {
                success: false,
                error: error.response?.data?.message || 'Ошибка запроса сброса пароля.',
            };
        }
    };

    // ── Сброс пароля: установка нового пароля ────────────────────────────────
    const resetPassword = async (token, newPassword) => {
        try {
            const response = await api.post('/auth/reset-password', { token, newPassword });
            applyAuthResponse(response.data);
            return { success: true };
        } catch (error) {
            return {
                success: false,
                error: error.response?.data?.message || 'Токен недействителен или истёк.',
            };
        }
    };

    // ── Активация фантомного аккаунта ─────────────────────────────────────────
    const claimPhantom = async (claimData) => {
        try {
            const response = await api.post('/auth/phantom/claim', claimData);
            const { userId, email } = response.data;
            return { success: true, userId, email };
        } catch (error) {
            return {
                success: false,
                error: error.response?.data?.message || 'Неверный код или email уже занят.',
            };
        }
    };

    // ── Выход ─────────────────────────────────────────────────────────────────
    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{
            user,
            login,
            register,
            verifyEmail,
            resendCode,
            forgotPassword,
            resetPassword,
            claimPhantom,
            logout,
            refreshUser,
        }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);
