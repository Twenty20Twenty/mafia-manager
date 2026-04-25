// src/components/ProtectedRoute.jsx
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Обёртка для роутов, требующих авторизации.
 * При попытке зайти без аутентификации — редиректит на /auth,
 * сохраняя текущий путь в state для последующего redirect after login.
 */
export function ProtectedRoute({ children }) {
    const { user } = useAuth();
    const location = useLocation();

    if (!user) {
        return <Navigate to="/auth" state={{ from: location }} replace />;
    }

    return children;
}

/**
 * Обёртка для роутов, требующих роли admin.
 * Неаутентифицированные → /auth.
 * Аутентифицированные без admin → / (главная).
 */
export function AdminRoute({ children }) {
    const { user } = useAuth();
    const location = useLocation();

    if (!user) {
        return <Navigate to="/auth" state={{ from: location }} replace />;
    }

    if (!user.isAdmin) {
        return <Navigate to="/" replace />;
    }

    return children;
}
