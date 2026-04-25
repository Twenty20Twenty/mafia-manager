// src/layout/MainLayout.jsx
import { useState, useEffect } from 'react';
import {
    AppShell, Group, Button, Text, Container, Burger,
    Drawer, Stack, Menu, Avatar, UnstyledButton
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Outlet, useNavigate, Link, useLocation } from 'react-router-dom';
import {
    IconShieldLock, IconLogout, IconSettings, IconUser,
    IconChevronDown, IconUsers
} from '@tabler/icons-react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import ThemeToggle from '../components/ThemeToggle';

export default function MainLayout() {
    const [opened, { toggle, close }] = useDisclosure(false);
    const navigate = useNavigate();
    const location = useLocation();
    const { user, logout } = useAuth();

    const [fullUser, setFullUser] = useState(null);
    const [userClub, setUserClub] = useState(null);

    useEffect(() => {
        const fetchUserData = async () => {
            if (!user?.id) { setFullUser(null); setUserClub(null); return; }
            try {
                const userRes = await api.get(`/users/${user.id}`);
                setFullUser(userRes.data);
                if (userRes.data.clubId) {
                    const clubRes = await api.get(`/clubs/${userRes.data.clubId}`);
                    setUserClub(clubRes.data);
                } else {
                    setUserClub(null);
                }
            } catch (error) {
                console.error("Ошибка загрузки данных в шапке", error);
            }
        };
        fetchUserData();
    }, [user?.id]);

    const mainLinks = [
        { link: '/clubs',       label: 'Клубы'    },
        { link: '/players',     label: 'Игроки'   },
        { link: '/tournaments', label: 'Турниры'  },
        { link: '/judges',      label: 'Судьи'    },
    ];

    const renderNavButtons = () =>
        mainLinks.map((link) => {
            const isActive = location.pathname.startsWith(link.link);
            return (
                <Button
                    key={link.label}
                    variant={isActive ? "outline" : "subtle"}
                    color={isActive ? "brandRed" : "gray"}
                    component={Link}
                    to={link.link}
                    onClick={close}
                >
                    {link.label}
                </Button>
            );
        });

    const displayAvatar   = fullUser?.avatarUrl  || user?.avatarUrl;
    const displayNickname = fullUser?.nickname   || user?.nickname;

    return (
        <AppShell header={{ height: 60 }} padding={{ base: 'xs', sm: 'md' }}>
            <AppShell.Header>
                <Container size="xl" h="100%" px={{ base: 'sm', sm: 'md' }}>
                    <Group justify="space-between" h="100%" wrap="nowrap">

                        <Group wrap="nowrap">
                            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" />
                            <Text
                                component={Link}
                                to="/"
                                fw={700}
                                size="lg"
                                c="brandRed.5"
                                style={{ textDecoration: 'none', whiteSpace: 'nowrap' }}
                            >
                                MafiaManager
                            </Text>
                        </Group>

                        {/* Навигация — только десктоп */}
                        <Group gap={5} visibleFrom="sm" wrap="nowrap">
                            {renderNavButtons()}
                        </Group>

                        {/* Правая часть: переключатель темы + пользователь */}
                        <Group wrap="nowrap" gap="xs">
                            {/* Кнопка переключения темы */}
                            <ThemeToggle />

                            {user ? (
                                <Menu
                                    trigger="hover"
                                    openDelay={100}
                                    closeDelay={400}
                                    position="bottom-end"
                                    withArrow
                                >
                                    <Menu.Target>
                                        <UnstyledButton
                                            component={Link}
                                            to={`/players/${user.id}`}
                                            py={4}
                                            px={8}
                                            style={{ borderRadius: 8, transition: 'background-color 0.2s' }}
                                            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--mantine-color-default-hover)'}
                                            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                                        >
                                            <Group gap={10} wrap="nowrap">
                                                <Avatar src={displayAvatar} size={36} radius="xl" color="brandRed">
                                                    {displayNickname?.[0]}
                                                </Avatar>
                                                <div style={{ flex: 1 }} className="hidden-xs">
                                                    <Text size="sm" fw={500} lh={1} visibleFrom="sm">
                                                        {displayNickname}
                                                    </Text>
                                                    <Text c="dimmed" size="xs" lh={1} mt={3} visibleFrom="sm">
                                                        {userClub ? userClub.name : 'Без клуба'}
                                                    </Text>
                                                </div>
                                                <IconChevronDown size={14} style={{ opacity: 0.5 }} />
                                            </Group>
                                        </UnstyledButton>
                                    </Menu.Target>

                                    <Menu.Dropdown>
                                        <Menu.Label>Аккаунт</Menu.Label>
                                        <Menu.Item leftSection={<IconUser size={14} />} component={Link} to={`/players/${user.id}`}>
                                            Мой профиль
                                        </Menu.Item>
                                        {user?.isAdmin && (
                                            <Menu.Item
                                                leftSection={<IconShieldLock size={14} />}
                                                component={Link} to="/admin"
                                                color="orange" style={{ fontWeight: 700 }}
                                            >
                                                Админ панель
                                            </Menu.Item>
                                        )}
                                        {userClub && (
                                            <Menu.Item
                                                leftSection={<IconUsers size={14} />}
                                                component={Link} to={`/clubs/${userClub.id}`}
                                                color="blue"
                                            >
                                                Мой клуб: {userClub.name}
                                            </Menu.Item>
                                        )}
                                        <Menu.Item leftSection={<IconSettings size={14} />} component={Link} to={`/players/${user.id}/edit`}>
                                            Настройки
                                        </Menu.Item>
                                        <Menu.Divider />
                                        <Menu.Item
                                            color="red"
                                            leftSection={<IconLogout size={14} />}
                                            onClick={() => { logout(); navigate('/'); }}
                                        >
                                            Выйти
                                        </Menu.Item>
                                    </Menu.Dropdown>
                                </Menu>
                            ) : (
                                <Button variant="default" onClick={() => navigate('/auth')}>Войти</Button>
                            )}
                        </Group>

                    </Group>
                </Container>
            </AppShell.Header>

            <AppShell.Main>
                <Container size="xl" px={{ base: 'xs', sm: 'md' }}>
                    <Outlet />
                </Container>
            </AppShell.Main>

            {/* Мобильное меню-drawer */}
            <Drawer opened={opened} onClose={close} size="75%" title="Меню" lockScroll={false}>
                <Stack gap="md">
                    {renderNavButtons()}
                    <ThemeToggle />
                    {user && (
                        <Button color="red" variant="subtle" onClick={() => { logout(); close(); }}>
                            Выйти
                        </Button>
                    )}
                </Stack>
            </Drawer>
        </AppShell>
    );
}
