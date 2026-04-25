// src/hooks/useAvatarUpload.js
import { useState, useEffect, useCallback } from 'react';

/**
 * Хук управления загрузкой аватара.
 * Инкапсулирует: выбор файла, предпросмотр, валидацию на клиенте, сброс.
 *
 * @param {string|null} initialUrl  - текущий URL аватара из БД
 * @returns {{
 *   file: File|null,
 *   previewUrl: string|null,
 *   displayUrl: string|null,
 *   error: string|null,
 *   handleFileSelect: function,
 *   reset: function,
 *   hasNewFile: boolean,
 * }}
 */
export function useAvatarUpload(initialUrl = null) {
    const [file, setFile]           = useState(null);
    const [previewUrl, setPreviewUrl] = useState(null);
    const [error, setError]         = useState(null);

    // Создаём object URL для предпросмотра и освобождаем при смене файла
    useEffect(() => {
        if (!file) {
            setPreviewUrl(null);
            return;
        }
        const url = URL.createObjectURL(file);
        setPreviewUrl(url);
        return () => URL.revokeObjectURL(url);
    }, [file]);

    const handleFileSelect = useCallback((selectedFile) => {
        setError(null);

        if (!selectedFile) {
            setFile(null);
            return;
        }

        // Клиентская валидация (сервер всё равно проверит, но даём быстрый фидбек)
        const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        const MAX_SIZE      = 5 * 1024 * 1024;

        if (!ALLOWED_TYPES.includes(selectedFile.type)) {
            setError('Допустимые форматы: JPEG, PNG, WebP, GIF');
            return;
        }
        if (selectedFile.size > MAX_SIZE) {
            setError('Максимальный размер файла: 5 МБ');
            return;
        }

        setFile(selectedFile);
    }, []);

    const reset = useCallback(() => {
        setFile(null);
        setError(null);
    }, []);

    return {
        file,
        previewUrl,
        // displayUrl: если есть предпросмотр — показываем его, иначе текущий из БД
        displayUrl: previewUrl ?? initialUrl,
        error,
        handleFileSelect,
        reset,
        hasNewFile: file !== null,
    };
}
