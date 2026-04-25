// src/pages/auth/LoginPage.jsx
import { useState } from 'react';
import { TextInput, PasswordInput, Button, Paper, Title, Container, Text, Anchor, Alert, Group } from '@mantine/core';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { IconAlertCircle, IconMailForward } from '@tabler/icons-react';

export default function LoginPage() {
    const [nickname, setNickname] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError]       = useState(null);
    const [loading, setLoading]   = useState(false);
    const [unverified, setUnverified] = useState(null); // { userId, email }
    const { login }  = useAuth();
    const navigate   = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setUnverified(null);
        setLoading(true);

        const result = await login(nickname, password);

        if (result.success) {
            navigate('/');
        } else if (result.emailNotVerified) {
            // Аккаунт есть, пароль верный — просто не подтверждён email
            setUnverified({ userId: result.userId, email: result.email });
            setLoading(false);
        } else {
            setError(result.error);
            setLoading(false);
        }
    };

    const handleGoVerify = () => {
        navigate(`/verify-email?userId=${unverified.userId}&email=${encodeURIComponent(unverified.email)}`);
    };

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" style={{ fontFamily: 'Verdana, sans-serif', fontWeight: 900 }}>
                Вход в систему
            </Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Нет аккаунта?{' '}
                <Anchor component={Link} to="/register" size="sm">
                    Зарегистрироваться
                </Anchor>
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                {unverified ? (
                    // Специальный экран — email не подтверждён
                    <Alert
                        icon={<IconMailForward size={18} />}
                        title="Email не подтверждён"
                        color="orange"
                    >
                        <Text size="sm" mb="md">
                            Вы зарегистрировались, но не подтвердили email{' '}
                            <strong>{unverified.email}</strong>.
                            Нажмите кнопку ниже — мы отправим новый код.
                        </Text>
                        <Button
                            fullWidth
                            color="orange"
                            leftSection={<IconMailForward size={16} />}
                            onClick={handleGoVerify}
                        >
                            Подтвердить email
                        </Button>
                        <Text ta="center" mt="sm" size="xs">
                            <Anchor onClick={() => setUnverified(null)} style={{ cursor: 'pointer' }}>
                                Назад ко входу
                            </Anchor>
                        </Text>
                    </Alert>
                ) : (
                    <form onSubmit={handleSubmit}>
                        {error && (
                            <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                                {error}
                            </Alert>
                        )}
                        <TextInput
                            label="Никнейм"
                            placeholder="Например: Фера"
                            required
                            value={nickname}
                            onChange={e => setNickname(e.currentTarget.value)}
                        />
                        <PasswordInput
                            label="Пароль"
                            placeholder="Ваш пароль"
                            required mt="md"
                            value={password}
                            onChange={e => setPassword(e.currentTarget.value)}
                        />

                        <Group justify="flex-end" mt="xs">
                            <Anchor component={Link} to="/forgot-password" size="xs" c="dimmed">
                                Забыли пароль?
                            </Anchor>
                        </Group>

                        <Button fullWidth mt="md" type="submit" color="brandRed" loading={loading}>
                            Войти
                        </Button>
                    </form>
                )}
            </Paper>
        </Container>
    );
}
