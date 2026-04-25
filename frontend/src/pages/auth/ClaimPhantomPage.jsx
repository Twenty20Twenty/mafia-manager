// src/pages/auth/ClaimPhantomPage.jsx
import { useState } from 'react';
import { Container, Title, TextInput, PasswordInput, Button, Paper, Text, Alert, Anchor } from '@mantine/core';
import { IconLink, IconInfoCircle, IconUser, IconAlertCircle } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function ClaimPhantomPage() {
    const [formData, setFormData] = useState({
        phantomCode:  '',
        nickname:     '',
        newEmail:     '',
        newPassword:  '',
        confirmPassword: '',
    });
    const [loading, setLoading] = useState(false);
    const [error, setError]     = useState(null);
    const navigate              = useNavigate();
    const { claimPhantom }      = useAuth();

    const set = (field, val) => setFormData(prev => ({ ...prev, [field]: val }));

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (formData.newPassword !== formData.confirmPassword) {
            return setError('Пароли не совпадают');
        }

        setLoading(true);
        const result = await claimPhantom({
            phantomCode:  formData.phantomCode,
            nickname:     formData.nickname,
            newEmail:     formData.newEmail,
            newPassword:  formData.newPassword,
        });

        if (result.success) {
            // Бэкенд вернул { userId, email } — нужно подтвердить email
            navigate(`/verify-email?userId=${result.userId}&email=${encodeURIComponent(result.email)}`);
        } else {
            setError(result.error);
            setLoading(false);
        }
    };

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" order={2}>Активация аккаунта</Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Введите код привязки, ник и задайте данные для входа.
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                <Alert variant="light" color="blue" icon={<IconInfoCircle size={16} />} mb="md">
                    Код привязки выдаётся организатором турнира. Введите никнейм точно так, как он записан в профиле.
                </Alert>

                {error && (
                    <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                        {error}
                    </Alert>
                )}

                <form onSubmit={handleSubmit}>
                    <TextInput
                        label="Никнейм фантома"
                        description="Точный игровой ник, под которым вы играли"
                        placeholder="Например: Твенти"
                        required
                        value={formData.nickname}
                        onChange={e => set('nickname', e.currentTarget.value)}
                        leftSection={<IconUser size={16} />}
                    />
                    <TextInput
                        label="Код привязки"
                        placeholder="Например: 8A4F9B2C"
                        required mt="md"
                        value={formData.phantomCode}
                        onChange={e => set('phantomCode', e.currentTarget.value.toUpperCase())}
                        leftSection={<IconLink size={16} />}
                    />
                    <TextInput
                        label="Ваш email"
                        placeholder="you@example.com"
                        type="email"
                        required mt="md"
                        value={formData.newEmail}
                        onChange={e => set('newEmail', e.currentTarget.value)}
                    />
                    <PasswordInput
                        label="Новый пароль"
                        placeholder="Минимум 6 символов"
                        required mt="md"
                        value={formData.newPassword}
                        onChange={e => set('newPassword', e.currentTarget.value)}
                    />
                    <PasswordInput
                        label="Повторите пароль"
                        placeholder="Повторите пароль"
                        required mt="md"
                        value={formData.confirmPassword}
                        onChange={e => set('confirmPassword', e.currentTarget.value)}
                    />

                    <Button fullWidth mt="xl" type="submit" loading={loading} color="brandRed">
                        Активировать аккаунт
                    </Button>
                </form>

                <Text ta="center" mt="md" size="xs" c="dimmed">
                    <Anchor component={Link} to="/auth">Уже есть аккаунт? Войти</Anchor>
                </Text>
            </Paper>
        </Container>
    );
}
