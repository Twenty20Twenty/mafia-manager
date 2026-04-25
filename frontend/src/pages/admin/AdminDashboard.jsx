// src/pages/admin/AdminDashboard.jsx
import { Container, Title, SimpleGrid, Paper, Text, ThemeIcon, Group } from '@mantine/core';
import { IconGavel, IconUserPlus, IconTrophy, IconPlus, IconBuildingFortress } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function AdminDashboard() {
    const { user } = useAuth();
    const c = useThemeColors();

    if (!user?.isAdmin) return <Container py="xl">Доступ запрещен</Container>;

    const items = [
        { title: 'Управление судьями',  icon: IconGavel,            color: 'violet', link: '/judges/manage'      },
        { title: 'Права клубов',         icon: IconTrophy,           color: 'orange', link: '/admin/clubs',        desc: 'Выдача статуса Турнирного оператора' },
        { title: 'Создать игрока',       icon: IconUserPlus,         color: 'blue',   link: '/admin/create-user',  desc: 'Регистрация фантомных профилей' },
        { title: 'Создать турнир',       icon: IconPlus,             color: 'green',  link: '/create-tournament',  desc: 'Запуск нового турнира от имени системы' },
        { title: 'Все пользователи',     icon: IconBuildingFortress, color: 'gray',   link: '/admin/users'         },
    ];

    return (
        <Container size="lg" py="xl">
            <Title order={2} mb="xl">Панель Администратора</Title>

            <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="lg">
                {items.map((item) => (
                    <Paper
                        key={item.title}
                        component={Link}
                        to={item.link}
                        withBorder p="md" radius="md"
                        style={{
                            textDecoration: 'none',
                            color: 'inherit',
                            transition: 'transform 0.2s, background-color 0.15s',
                            backgroundColor: c.surface2,
                        }}
                        onMouseEnter={e => {
                            e.currentTarget.style.transform = 'scale(1.02)';
                            e.currentTarget.style.backgroundColor = c.surface3;
                        }}
                        onMouseLeave={e => {
                            e.currentTarget.style.transform = 'scale(1)';
                            e.currentTarget.style.backgroundColor = c.surface2;
                        }}
                    >
                        <Group align="flex-start">
                            <ThemeIcon size="xl" color={item.color} variant="light" style={{ flexShrink: 0 }}>
                                <item.icon size={22} />
                            </ThemeIcon>
                            <div style={{ minWidth: 0 }}>
                                <Text fw={700} size="lg" style={{ wordBreak: 'break-word' }}>{item.title}</Text>
                                {item.desc && <Text size="xs" c="dimmed">{item.desc}</Text>}
                            </div>
                        </Group>
                    </Paper>
                ))}
            </SimpleGrid>
        </Container>
    );
}
