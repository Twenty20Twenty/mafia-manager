// src/pages/players/EditPlayerPage.jsx
import { useEffect, useState } from 'react';
import {
    Container, Title, Button, Paper, Group,
    Select, Center, Loader, Alert, TextInput
} from '@mantine/core';
import { IconArrowLeft, IconDeviceFloppy, IconAlertCircle } from '@tabler/icons-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { uploadUserAvatar } from '../../api/avatar';
import { useAvatarUpload } from '../../hooks/useAvatarUpload';
import AvatarUpload from '../../components/AvatarUpload';

export default function EditPlayerPage() {
    const { id }     = useParams();
    const navigate   = useNavigate();
    const { user }   = useAuth();

    const [formData, setFormData]   = useState(null);
    const [cities, setCities]       = useState([]);
    const [loading, setLoading]     = useState(true);
    const [saving, setSaving]       = useState(false);
    const [error, setError]         = useState(null);

    const canEdit = user && (user.id === Number(id) || user.isAdmin);

    // Хук управления аватаром
    const avatar = useAvatarUpload(formData?.avatarUrl ?? null);

    useEffect(() => {
        if (!canEdit) { setLoading(false); return; }

        const fetchData = async () => {
            try {
                const [playerRes, citiesRes] = await Promise.all([
                    api.get(`/users/${id}`),
                    api.get('/geo/cities'),
                ]);
                setFormData({
                    nickname:  playerRes.data.nickname  || '',
                    city:      playerRes.data.city      || '',
                    avatarUrl: playerRes.data.avatarUrl || null,
                });
                setCities(citiesRes.data.map(c => ({ value: c.name, label: c.name })));
            } catch (err) {
                console.error('Ошибка загрузки профиля', err);
                setError('Не удалось загрузить профиль');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [id, canEdit]); // eslint-disable-line react-hooks/exhaustive-deps

    if (!canEdit) {
        return <Container py="xl"><Alert color="red">Доступ запрещен</Alert></Container>;
    }
    if (loading) {
        return <Center py="xl" mt="xl"><Loader color="brandRed" /></Center>;
    }
    if (error || !formData) {
        return <Container py="xl"><Alert color="red" icon={<IconAlertCircle />}>{error || 'Профиль не найден'}</Alert></Container>;
    }

    const set = (field, val) => setFormData(prev => ({ ...prev, [field]: val }));

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError(null);
        try {
            // 1. Загружаем новый аватар если выбран
            if (avatar.hasNewFile) {
                await uploadUserAvatar(Number(id), avatar.file);
            }

            // 2. Обновляем профиль (nickname, city)
            await api.put(`/users/${id}`, {
                nickname: formData.nickname,
                city:     formData.city,
            });

            navigate(`/players/${id}`);
        } catch (err) {
            setError(err.response?.data?.message || 'Ошибка сохранения профиля');
        } finally {
            setSaving(false);
        }
    };

    return (
        <Container size="sm" py="xl">
            <Button
                component={Link} to={`/players/${id}`}
                variant="subtle" color="gray"
                leftSection={<IconArrowLeft size={16} />}
                mb="md"
            >
                Назад в профиль
            </Button>

            <Title order={2} mb="lg">Редактирование профиля</Title>

            {error && (
                <Alert color="red" icon={<IconAlertCircle />} mb="md">{error}</Alert>
            )}

            <Paper withBorder p={{ base: 'md', sm: 'xl' }} radius="md">
                <form onSubmit={handleSubmit}>
                    {/* Аватар */}
                    <Group justify="center" mb="xl">
                        <AvatarUpload
                            displayUrl={avatar.displayUrl}
                            nickname={formData.nickname}
                            onFileSelect={avatar.handleFileSelect}
                            onReset={avatar.reset}
                            hasNewFile={avatar.hasNewFile}
                            error={avatar.error}
                            loading={saving}
                            size={100}
                        />
                    </Group>

                    <TextInput
                        label="Никнейм"
                        description={!user.isAdmin ? "Смена ника доступна раз в 30 дней" : undefined}
                        value={formData.nickname}
                        onChange={e => set('nickname', e.currentTarget.value)}
                        disabled={!user.isAdmin}
                        mb="md"
                    />

                    <Select
                        label="Город"
                        data={cities}
                        searchable
                        placeholder="Выберите город"
                        value={formData.city}
                        onChange={val => set('city', val)}
                        mb="md"
                    />

                    <Group justify="flex-end" mt="xl" grow hiddenFrom="sm">
                        <Button variant="default" onClick={() => navigate(`/players/${id}`)}>
                            Отмена
                        </Button>
                        <Button type="submit" leftSection={<IconDeviceFloppy size={16} />} loading={saving}>
                            Сохранить
                        </Button>
                    </Group>
                    <Group justify="flex-end" mt="xl" visibleFrom="sm">
                        <Button variant="default" onClick={() => navigate(`/players/${id}`)}>
                            Отмена
                        </Button>
                        <Button type="submit" leftSection={<IconDeviceFloppy size={16} />} loading={saving}>
                            Сохранить
                        </Button>
                    </Group>
                </form>
            </Paper>
        </Container>
    );
}
