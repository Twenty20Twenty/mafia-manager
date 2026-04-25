// src/pages/admin/ManageUsersPage.jsx
import { useState, useEffect } from 'react';
import {
    Container, Title, Paper, Group, Button, TextInput, Table, Avatar,
    Badge, Modal, Text, Center, Loader, Pagination, Alert, Stack, Box
} from '@mantine/core';
import {
    IconSearch, IconPencil, IconKey, IconArrowLeft,
    IconGhost, IconUser, IconShieldLock
} from '@tabler/icons-react';
import { useDisclosure } from '@mantine/hooks';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function ManageUsersPage() {
    const { user } = useAuth();
    const c = useThemeColors();

    const [search, setSearch]         = useState('');
    const [users, setUsers]           = useState([]);
    const [loading, setLoading]       = useState(true);
    const [activePage, setPage]       = useState(1);
    const [totalPages, setTotalPages] = useState(1);

    const [opened, { open, close }]     = useDisclosure(false);
    const [editingUser, setEditingUser] = useState(null);
    const [newNickname, setNewNickname] = useState('');
    const [saving, setSaving]           = useState(false);

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const res = await api.get('/users', { params: { search, page: activePage - 1, size: 10 } });
            setUsers(res.data.content);
            setTotalPages(res.data.totalPages);
        } catch (error) {
            console.error("Ошибка загрузки", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user?.isAdmin) {
            const timer = setTimeout(fetchUsers, 300);
            return () => clearTimeout(timer);
        }
    }, [search, activePage, user]); // eslint-disable-line

    if (!user?.isAdmin) {
        return (
            <Container py="xl">
                <Alert color="red" icon={<IconShieldLock />}>Доступ запрещен. Только для администраторов.</Alert>
            </Container>
        );
    }

    const saveNickname = async () => {
        if (!newNickname.trim()) return;
        setSaving(true);
        try {
            await api.put(`/users/${editingUser.id}`, { nickname: newNickname });
            alert(`Никнейм изменен на ${newNickname}`);
            close();
            fetchUsers();
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка смены никнейма');
        } finally {
            setSaving(false);
        }
    };

    const showPhantomCode = (u) => {
        if (u.phantomCode) alert(`Код привязки для ${u.nickname}:\n\n${u.phantomCode}`);
        else alert(`У пользователя ${u.nickname} нет кода привязки.`);
    };

    const openEdit = (u) => { setEditingUser(u); setNewNickname(u.nickname); open(); };

    return (
        <Container size="lg" py="xl">
            <Button component={Link} to="/admin" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />} mb="md">
                В панель
            </Button>
            <Title order={2} mb="xl">Управление пользователями</Title>

            <Paper withBorder p="md" radius="md" style={{ backgroundColor: c.surface2 }}>
                <TextInput
                    placeholder="Поиск игрока..."
                    leftSection={<IconSearch size={16} />}
                    mb="md"
                    value={search}
                    onChange={e => { setSearch(e.currentTarget.value); setPage(1); }}
                />

                {loading ? (
                    <Center py="xl"><Loader color="brandRed" /></Center>
                ) : (
                    <>
                        {/* Десктоп */}
                        <Box visibleFrom="sm">
                            <Table striped highlightOnHover
                                styles={{ thead: { backgroundColor: c.tableHeader } }}>
                                <Table.Thead>
                                    <Table.Tr>
                                        <Table.Th>Пользователь</Table.Th>
                                        <Table.Th>Тип</Table.Th>
                                        <Table.Th>Действия</Table.Th>
                                    </Table.Tr>
                                </Table.Thead>
                                <Table.Tbody>
                                    {users.map(u => (
                                        <Table.Tr key={u.id}>
                                            <Table.Td>
                                                <Group gap="sm" wrap="nowrap">
                                                    <Avatar src={u.avatarUrl} size={32} radius="xl" color="brandRed">{u.nickname[0]}</Avatar>
                                                    <div>
                                                        <Text size="sm" fw={500}>{u.nickname}</Text>
                                                        <Text size="xs" c="dimmed">{u.city || 'Город не указан'}</Text>
                                                    </div>
                                                </Group>
                                            </Table.Td>
                                            <Table.Td>
                                                {u.isPhantom
                                                    ? <Badge color="gray" variant="light" leftSection={<IconGhost size={12} />}>Фантом</Badge>
                                                    : <Badge color="blue" variant="light" leftSection={<IconUser size={12} />}>Реальный</Badge>
                                                }
                                            </Table.Td>
                                            <Table.Td>
                                                <Group gap="xs">
                                                    <Button size="xs" variant="light" leftSection={<IconPencil size={14} />} onClick={() => openEdit(u)}>Ник</Button>
                                                    {u.isPhantom && (
                                                        <Button size="xs" color="orange" variant="light" leftSection={<IconKey size={14} />} onClick={() => showPhantomCode(u)}>Код</Button>
                                                    )}
                                                </Group>
                                            </Table.Td>
                                        </Table.Tr>
                                    ))}
                                </Table.Tbody>
                            </Table>
                        </Box>

                        {/* Мобилка */}
                        <Stack gap="sm" hiddenFrom="sm">
                            {users.map(u => (
                                <Paper key={u.id} withBorder p="sm" radius="sm"
                                    style={{ backgroundColor: c.surface3 }}>
                                    <Group justify="space-between" wrap="nowrap">
                                        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
                                            <Avatar src={u.avatarUrl} size={36} radius="xl" color="brandRed" style={{ flexShrink: 0 }}>
                                                {u.nickname[0]}
                                            </Avatar>
                                            <div style={{ minWidth: 0 }}>
                                                <Text size="sm" fw={500} truncate>{u.nickname}</Text>
                                                <Group gap={4} mt={2}>
                                                    <Text size="xs" c="dimmed">{u.city || 'Город не указан'}</Text>
                                                    {u.isPhantom
                                                        ? <Badge color="gray" variant="light" size="xs" leftSection={<IconGhost size={10} />}>Фантом</Badge>
                                                        : <Badge color="blue" variant="light" size="xs" leftSection={<IconUser size={10} />}>Реальный</Badge>
                                                    }
                                                </Group>
                                            </div>
                                        </Group>
                                        <Group gap="xs" style={{ flexShrink: 0 }}>
                                            <Button size="xs" variant="light" leftSection={<IconPencil size={14} />} onClick={() => openEdit(u)}>Ник</Button>
                                            {u.isPhantom && (
                                                <Button size="xs" color="orange" variant="light" leftSection={<IconKey size={14} />} onClick={() => showPhantomCode(u)}>Код</Button>
                                            )}
                                        </Group>
                                    </Group>
                                </Paper>
                            ))}
                        </Stack>

                        {totalPages > 1 && (
                            <Center mt="xl">
                                <Pagination total={totalPages} value={activePage} onChange={setPage} color="brandRed" />
                            </Center>
                        )}
                    </>
                )}
            </Paper>

            <Modal opened={opened} onClose={close} title="Смена никнейма (Админ)" centered>
                <TextInput label="Новый никнейм" value={newNickname} onChange={e => setNewNickname(e.currentTarget.value)} mb="md" />
                <Button fullWidth onClick={saveNickname} color="brandRed" loading={saving}>Сохранить</Button>
            </Modal>
        </Container>
    );
}
