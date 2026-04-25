// src/pages/HomePage.jsx
import { Title, Text, Button, Container, SimpleGrid, ThemeIcon, Paper, Group } from '@mantine/core';
import { IconTrophy, IconUsers, IconGavel, IconUser, IconSearch } from '@tabler/icons-react';
import { useNavigate, Link } from 'react-router-dom'; // Добавил Link
import { useAuth } from '../context/AuthContext'; // Импорт контекста

export default function HomePage() {
    const navigate = useNavigate();
    const { user } = useAuth(); // Получаем пользователя

    return (
        <Container size="lg" py="xl">
            <div style={{ textAlign: 'center', marginBottom: 50, marginTop: 30 }}>
                <Title order={1} size={48} weight={900} c="brandRed.5">
                    Mafia Manager
                </Title>
                <Text c="dimmed" size="xl" mt="md" mx="auto" maw={600}>
                    Профессиональная платформа для управления турнирами по спортивной мафии.
                </Text>

                {/* ЛОГИКА КНОПОК */}
                <Group justify="center" mt="xl">
                    {!user ? (
                        <Button size="lg" radius="md" onClick={() => navigate('/auth')}>
                            Начать играть
                        </Button>
                    ) : (
                        <>
                            <Button
                                component={Link}
                                to={`/players/${user.id}`}
                                size="lg"
                                radius="md"
                                leftSection={<IconUser size={20}/>}
                                color="brandRed"
                            >
                                Мой профиль
                            </Button>
                            <Button
                                component={Link}
                                to="/tournaments"
                                size="lg"
                                radius="md"
                                leftSection={<IconSearch size={20}/>}
                                variant="default"
                            >
                                Найти турнир
                            </Button>
                        </>
                    )}
                </Group>
            </div>

            {/* Карточки особенностей (без изменений) */}
            <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="xl" mt={50}>
                <FeatureCard
                    icon={<IconTrophy size={26} />}
                    title="Турниры и Рейтинги"
                    description="Участвуйте в краткосрочных турнирах или длительных рейтинговых сезонах."
                />
                <FeatureCard
                    icon={<IconUsers size={26} />}
                    title="Клубы"
                    description="Вступайте в клубы, создавайте свои сообщества."
                />
                <FeatureCard
                    icon={<IconGavel size={26} />}
                    title="Цифровые протоколы"
                    description="Судьи ведут игру онлайн. Система сама считает ЛХ и баллы."
                />
            </SimpleGrid>
        </Container>
    );
}

function FeatureCard({ icon, title, description }) {
    return (
        <Paper shadow="md" p="xl" radius="md" withBorder>
            <ThemeIcon size="xl" radius="md" variant="gradient" gradient={{ from: 'brandRed', to: 'red' }}>
                {icon}
            </ThemeIcon>
            <Text size="lg" fw={500} mt="md">
                {title}
            </Text>
            <Text size="sm" mt="sm" c="dimmed">
                {description}
            </Text>
        </Paper>
    );
}
