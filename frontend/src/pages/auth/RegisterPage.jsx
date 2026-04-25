// src/pages/auth/RegisterPage.jsx
import { useEffect, useState } from 'react';
import { TextInput, PasswordInput, Button, Paper, Title, Container, Text, Anchor, Select, Alert } from '@mantine/core';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { IconAlertCircle } from '@tabler/icons-react';
import api from '../../api/axios.js';

export default function RegisterPage() {
    const { register } = useAuth();
    const navigate      = useNavigate();

    const [cities, setCities]   = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError]     = useState(null);
    const [formData, setFormData] = useState({
        nickname:        '',
        email:           '',
        password:        '',
        confirmPassword: '',
        city:            '',
        gender:          'male',
    });

    useEffect(() => {
        api.get('/geo/cities')
            .then(res => setCities(res.data.map(c => ({ value: c.name, label: c.name }))))
            .catch(err => console.error('Ошибка загрузки городов', err));
    }, []);

    const handleChange = (field, value) => setFormData(prev => ({ ...prev, [field]: value }));

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (formData.password !== formData.confirmPassword) {
            return setError('Пароли не совпадают!');
        }

        setLoading(true);
        const result = await register({
            nickname: formData.nickname,
            email:    formData.email,
            password: formData.password,
            city:     formData.city,
            gender:   formData.gender,
        });

        if (result.success) {
            // Бэкенд вернул { userId, email } — JWT ещё нет.
            // Перенаправляем на страницу ввода кода из письма.
            navigate(`/verify-email?userId=${result.userId}&email=${encodeURIComponent(result.email)}`);
        } else {
            setError(result.error);
            setLoading(false);
        }
    };

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" fw={900}>Регистрация игрока</Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Уже есть аккаунт?{' '}
                <Anchor component={Link} to="/auth" size="sm">Войти</Anchor>
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                <form onSubmit={handleSubmit}>
                    {error && (
                        <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                            {error}
                        </Alert>
                    )}

                    <TextInput
                        label="Никнейм" placeholder="MafiaKing" required
                        value={formData.nickname}
                        onChange={e => handleChange('nickname', e.currentTarget.value)}
                    />
                    <TextInput
                        label="Email" placeholder="you@example.com" mt="md" required
                        value={formData.email}
                        onChange={e => handleChange('email', e.currentTarget.value)}
                    />
                    <Select
                        label="Город" placeholder="Выберите город"
                        data={cities} searchable mt="md"
                        value={formData.city}
                        onChange={val => handleChange('city', val)}
                    />
                    <Select
                        label="Пол"
                        data={[
                            { value: 'male',   label: 'Мужской' },
                            { value: 'female', label: 'Женский' },
                        ]}
                        mt="md"
                        value={formData.gender}
                        onChange={val => handleChange('gender', val)}
                    />
                    <PasswordInput
                        label="Пароль" placeholder="Создайте пароль" mt="md" required
                        value={formData.password}
                        onChange={e => handleChange('password', e.currentTarget.value)}
                    />
                    <PasswordInput
                        label="Повторите пароль" placeholder="Повторите пароль" mt="md" required
                        value={formData.confirmPassword}
                        onChange={e => handleChange('confirmPassword', e.currentTarget.value)}
                    />

                    <Button fullWidth mt="xl" type="submit" loading={loading} color="brandRed">
                        Создать аккаунт
                    </Button>
                </form>

                <Text ta="center" mt="md" size="xs">
                    Есть код привязки?{' '}
                    <Anchor component={Link} to="/claim-phantom">Активировать</Anchor>
                </Text>
            </Paper>
        </Container>
    );
}
