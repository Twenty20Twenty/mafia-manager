// src/pages/admin/CreatePhantomPage.jsx
import { Container, Title, TextInput, Button, Paper, Group, Select, Alert } from '@mantine/core';
import { IconArrowLeft, IconUserPlus, IconInfoCircle, IconShieldLock } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';

export default function CreatePhantomPage() {
    const { user }     = useAuth();
    const navigate     = useNavigate();
    const [formData, setFormData] = useState({ nickname: '', city: '', gender: 'male' });
    const [cities, setCities]     = useState([]);
    const [loading, setLoading]   = useState(false);

    useEffect(() => {
        if (user?.isAdmin) {
            api.get('/geo/cities')
                .then(res => setCities(res.data.map(c => ({ value: c.name, label: c.name }))))
                .catch(console.error);
        }
    }, [user]);

    // Защита роли
    if (!user?.isAdmin) {
        return (
            <Container py="xl">
                <Alert color="red" icon={<IconShieldLock />}>
                    Доступ запрещен. Только для администраторов.
                </Alert>
            </Container>
        );
    }

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await api.post('/admin/phantom/create', formData);
            alert(`Пользователь "${res.data.nickname}" создан!\n\nСгенерирован код привязки: ${res.data.code}`);
            navigate('/admin');
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка при создании фантома');
        } finally {
            setLoading(false);
        }
    };

    const set = (field, val) => setFormData(prev => ({ ...prev, [field]: val }));

    return (
        <Container size="sm" py="xl">
            <Button component={Link} to="/admin" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />} mb="md">
                Вернуться в панель
            </Button>

            <Title order={2} mb="lg">Создание фантомного профиля</Title>

            <Alert variant="light" color="blue" title="Что такое фантомный профиль?" icon={<IconInfoCircle />} mb="xl">
                Это профиль игрока, который участвует в турнирах, но еще не зарегистрировался на сайте.
            </Alert>

            {/* p: меньше на мобилке */}
            <Paper withBorder p={{ base: 'md', sm: 'xl' }} radius="md">
                <form onSubmit={handleSubmit}>
                    <TextInput
                        label="Никнейм" required
                        value={formData.nickname}
                        onChange={e => set('nickname', e.currentTarget.value)}
                    />
                    <Select
                        label="Город" mt="md" searchable
                        data={cities}
                        value={formData.city}
                        onChange={val => set('city', val)}
                    />
                    <Select
                        label="Пол" mt="md"
                        data={[
                            { value: 'male',   label: 'Мужской' },
                            { value: 'female', label: 'Женский' },
                        ]}
                        value={formData.gender}
                        onChange={val => set('gender', val)}
                    />
                    {/* justify="flex-end" + fullWidth на мобилке через Button fullWidth */}
                    <Group justify="flex-end" mt="xl">
                        <Button
                            type="submit"
                            loading={loading}
                            leftSection={<IconUserPlus size={16} />}
                            color="brandRed"
                        >
                            Создать профиль
                        </Button>
                    </Group>
                </form>
            </Paper>
        </Container>
    );
}
