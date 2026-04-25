// src/pages/auth/ForgotPasswordPage.jsx
import { useState } from 'react';
import { TextInput, Button, Paper, Title, Container, Text, Anchor, Alert } from '@mantine/core';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { IconAlertCircle, IconCircleCheck } from '@tabler/icons-react';

export default function ForgotPasswordPage() {
    const { forgotPassword } = useAuth();

    const [email, setEmail]   = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError]   = useState(null);
    const [sent, setSent]     = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        const result = await forgotPassword(email);
        setLoading(false);

        if (result.success) {
            setSent(true);
        } else {
            setError(result.error);
        }
    };

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" fw={900}>Сброс пароля</Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Вспомнили пароль?{' '}
                <Anchor component={Link} to="/auth" size="sm">Войти</Anchor>
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                {sent ? (
                    <Alert icon={<IconCircleCheck size={16} />} title="Письмо отправлено!" color="green">
                        Если аккаунт с адресом <strong>{email}</strong> существует — ссылка для сброса
                        пароля уже у вас в почте. Проверьте папку «Спам», если письмо не пришло.
                    </Alert>
                ) : (
                    <form onSubmit={handleSubmit}>
                        {error && (
                            <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                                {error}
                            </Alert>
                        )}

                        <Text size="sm" c="dimmed" mb="md">
                            Введите email, указанный при регистрации. Мы отправим ссылку для сброса пароля.
                        </Text>

                        <TextInput
                            label="Email"
                            placeholder="you@example.com"
                            type="email"
                            required
                            value={email}
                            onChange={e => setEmail(e.currentTarget.value)}
                        />

                        <Button fullWidth mt="xl" type="submit" loading={loading} color="brandRed">
                            Отправить ссылку
                        </Button>
                    </form>
                )}
            </Paper>
        </Container>
    );
}
