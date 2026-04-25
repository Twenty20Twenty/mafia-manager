// src/api/avatar.js
import api from './axios';

/**
 * Загружает аватар пользователя на сервер.
 * @param {number} userId
 * @param {File} file
 * @returns {Promise<string>} новый avatarUrl
 */
export const uploadUserAvatar = async (userId, file) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post(`/users/${userId}/avatar`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });

    return response.data.avatarUrl;
};

/**
 * Удаляет аватар пользователя.
 * @param {number} userId
 */
export const deleteUserAvatar = async (userId) => {
    await api.delete(`/users/${userId}/avatar`);
};

/**
 * Загружает логотип клуба.
 * @param {number} clubId
 * @param {File} file
 * @returns {Promise<string>} новый logoUrl
 */
export const uploadClubAvatar = async (clubId, file) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post(`/clubs/${clubId}/avatar`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });

    return response.data.avatarUrl;
};

/**
 * Удаляет логотип клуба.
 * @param {number} clubId
 */
export const deleteClubAvatar = async (clubId) => {
    await api.delete(`/clubs/${clubId}/avatar`);
};
