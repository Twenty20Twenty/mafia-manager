// src/pages/auth/ResetPasswordPage.jsx
import { useState } from 'react';
import { PasswordInput, Button, Paper, Title, Container, Text, Anchor, Alert } from '@mantine/core';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { IconAlertCircle } from '@tabler/icons-react';

export default function ResetPasswordPage() {
    const { resetPassword } = useAuth();
    const navigate          = useNavigate();
    const [searchParams]    = useSearchParams();

    const token = searchParams.get('token');

    const [newPassword, setNewPassword]         = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading]                 = useState(false);
    const [error, setError]                     = useState(null);

    if (!token) {
        return (
            <Container size={420} my={40}>
                <Alert color="red" title="Недействительная ссылка" icon={<IconAlertCircle size={16} />}>
                    Токен сброса пароля отсутствует или ссылка устарела.{' '}
                    <Anchor component={Link} to="/forgot-password">Запросить новую ссылку.</Anchor>
                </Alert>
            </Container>
        );
    }

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        if (newPassword.length < 6) {
            return setError('Пароль должен содержать минимум 6 символов');
        }
        if (newPassword !== confirmPassword) {
            return setError('Пароли не совпадают');
        }

        setLoading(true);
        const result = await resetPassword(token, newPassword);

        if (result.success) {
            navigate('/');
        } else {
            setError(result.error);
            setLoading(false);
        }
    };

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" fw={900}>Новый пароль</Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Придумайте надёжный пароль для вашего аккаунта
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                <form onSubmit={handleSubmit}>
                    {error && (
                        <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                            {error}
                        </Alert>
                    )}

                    <PasswordInput
                        label="Новый пароль"
                        placeholder="Минимум 6 символов"
                        required
                        value={newPassword}
                        onChange={e => setNewPassword(e.currentTarget.value)}
                    />
                    <PasswordInput
                        label="Повторите пароль"
                        placeholder="Повторите пароль"
                        mt="md"
                        required
                        value={confirmPassword}
                        onChange={e => setConfirmPassword(e.currentTarget.value)}
                    />

                    <Button fullWidth mt="xl" type="submit" loading={loading} color="brandRed">
                        Сохранить пароль
                    </Button>
                </form>

                <Text ta="center" mt="md" size="xs">
                    <Anchor component={Link} to="/forgot-password">Запросить новую ссылку</Anchor>
                </Text>
            </Paper>
        </Container>
    );
}
