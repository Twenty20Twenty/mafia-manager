// src/pages/auth/VerifyEmailPage.jsx
import { useState } from 'react';
import { PinInput, Button, Paper, Title, Container, Text, Anchor, Alert, Group } from '@mantine/core';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { IconAlertCircle, IconCircleCheck, IconShieldOff } from '@tabler/icons-react';

export default function VerifyEmailPage() {
    const { verifyEmail, resendCode } = useAuth();
    const navigate                    = useNavigate();
    const [searchParams]              = useSearchParams();

    const userId = searchParams.get('userId');
    const email  = searchParams.get('email') || 'вашу почту';

    const [code, setCode]           = useState('');
    const [loading, setLoading]     = useState(false);
    const [resending, setResending] = useState(false);
    const [error, setError]         = useState(null);
    const [resendOk, setResendOk]   = useState(false);
    const [isPhantom, setIsPhantom] = useState(false);

    const handleVerify = async () => {
        if (code.length !== 6) return setError('Введите 6-значный код');
        setError(null);
        setLoading(true);

        const result = await verifyEmail(Number(userId), code);
        if (result.success) {
            navigate('/');
        } else {
            // 403 с сообщением про фантома — отдельный экран
            if (result.phantom) setIsPhantom(true);
            else setError(result.error);
            setLoading(false);
        }
    };

    const handleResend = async () => {
        setResendOk(false);
        setError(null);
        setResending(true);

        const result = await resendCode(Number(userId));
        setResending(false);

        if (result.success) {
            setResendOk(true);
        } else {
            if (result.phantom) setIsPhantom(true);
            else setError(result.error);
        }
    };

    // Некорректный URL (нет userId)
    if (!userId) {
        return (
            <Container size={420} my={40}>
                <Alert color="red" title="Ошибка" icon={<IconAlertCircle size={16} />}>
                    Неверная ссылка.{' '}
                    <Anchor component={Link} to="/register">Зарегистрируйтесь заново.</Anchor>
                </Alert>
            </Container>
        );
    }

    // Попытка использовать verify-email для фантомного аккаунта
    if (isPhantom) {
        return (
            <Container size={420} my={40}>
                <Alert color="orange" title="Фантомный аккаунт" icon={<IconShieldOff size={16} />}>
                    <Text size="sm" mb="md">
                        Этот аккаунт является фантомным и активируется только через код привязки,
                        а не через email-подтверждение.
                    </Text>
                    <Button component={Link} to="/claim-phantom" color="orange" fullWidth>
                        Активировать через код привязки
                    </Button>
                </Alert>
            </Container>
        );
    }

    return (
        <Container size={420} my={{ base: 'md', sm: 40 }}>
            <Title ta="center" fw={900}>Подтвердите email</Title>
            <Text c="dimmed" size="sm" ta="center" mt={5}>
                Мы отправили 6-значный код на{' '}
                <Text component="span" fw={600} c="brandRed.5">{email}</Text>
            </Text>

            <Paper withBorder shadow="md" p={{ base: 'md', sm: 30 }} mt={30} radius="md">
                {error && (
                    <Alert icon={<IconAlertCircle size={16} />} title="Ошибка!" color="red" mb="md">
                        {error}
                    </Alert>
                )}
                {resendOk && (
                    <Alert icon={<IconCircleCheck size={16} />} title="Готово!" color="green" mb="md">
                        Новый код отправлен на {email}
                    </Alert>
                )}

                <Text size="sm" mb="xs" c="dimmed">Введите код из письма:</Text>

                <Group justify="center">
                    <PinInput
                        length={6}
                        type="number"
                        value={code}
                        onChange={setCode}
                        onComplete={handleVerify}
                        size="lg"
                        autoFocus
                    />
                </Group>

                <Button
                    fullWidth mt="xl"
                    loading={loading}
                    color="brandRed"
                    onClick={handleVerify}
                    disabled={code.length !== 6}
                >
                    Подтвердить
                </Button>

                <Text ta="center" mt="md" size="sm">
                    Не получили письмо?{' '}
                    <Anchor onClick={handleResend} style={{ cursor: 'pointer' }}>
                        {resending ? 'Отправляем...' : 'Отправить повторно'}
                    </Anchor>
                </Text>

                <Text ta="center" mt="xs" size="xs" c="dimmed">
                    <Anchor component={Link} to="/register">Назад к регистрации</Anchor>
                </Text>
            </Paper>
        </Container>
    );
}
