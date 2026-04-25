// src/pages/admin/ManageClubRightsPage.jsx
import { useState, useEffect } from 'react';
import { Container, Title, Paper, Group, Button, Select, Text, Switch, Stack, Divider, Avatar, Alert } from '@mantine/core';
import { IconArrowLeft, IconSearch, IconTrophy, IconDeviceFloppy, IconShieldLock } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function ManageClubRightsPage() {
    const { user } = useAuth();
    const c = useThemeColors();

    const [clubs, setClubs]               = useState([]);
    const [selectedClubId, setSelectedClubId] = useState(null);
    const [isOperator, setIsOperator]     = useState(false);
    const [loading, setLoading]           = useState(false);

    useEffect(() => {
        if (user?.isAdmin) {
            api.get('/clubs').then(res => setClubs(res.data)).catch(console.error);
        }
    }, [user]);

    if (!user?.isAdmin) {
        return (
            <Container py="xl">
                <Alert color="red" icon={<IconShieldLock />}>Доступ запрещен. Требуются права администратора.</Alert>
            </Container>
        );
    }

    const selectData = clubs.map(cl => ({
        value: String(cl.id),
        label: cl.name,
        logo: cl.logoUrl,
        isTournamentOperator: cl.isTournamentOperator,
    }));

    const renderSelectOption = ({ option }) => (
        <Group justify="space-between" w="100%">
            <Group gap="sm">
                <Avatar src={option.logo} size={24} radius="md" />
                <Text size="sm">{option.label}</Text>
            </Group>
            {option.isTournamentOperator && <IconTrophy size={16} color="var(--mantine-color-orange-5)" />}
        </Group>
    );

    const handleSelectClub = (value) => {
        setSelectedClubId(value);
        const club = clubs.find(cl => cl.id === Number(value));
        if (club) setIsOperator(club.isTournamentOperator || false);
    };

    const handleSave = async () => {
        setLoading(true);
        try {
            await api.put(`/admin/clubs/${selectedClubId}/operator?status=${isOperator}`);
            alert('Права клуба обновлены!');
            setClubs(clubs.map(cl =>
                cl.id === Number(selectedClubId) ? { ...cl, isTournamentOperator: isOperator } : cl
            ));
        } catch {
            alert('Ошибка обновления прав');
        } finally {
            setLoading(false);
        }
    };

    const isDisabled = !selectedClubId;

    return (
        <Container size="sm" py="xl">
            <Button component={Link} to="/admin" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />} mb="md">
                В админ-панель
            </Button>
            <Title order={2} mb="xl">Права клубов</Title>

            <Paper withBorder p="xl" radius="md" style={{ backgroundColor: c.surface2 }}>
                <Select
                    label="Поиск клуба" placeholder="Название клуба..."
                    data={selectData} renderOption={renderSelectOption}
                    searchable leftSection={<IconSearch size={16} />} size="md" mb="xl"
                    onChange={handleSelectClub} value={selectedClubId}
                />
                <Divider mb="xl" />
                <Stack gap="lg">
                    <Group
                        justify="space-between" p="sm"
                        style={{
                            borderRadius: 8,
                            backgroundColor: isDisabled ? 'transparent' : c.surface3,
                        }}
                    >
                        <Group>
                            <IconTrophy size={24} color={isDisabled ? 'gray' : 'orange'} />
                            <div>
                                <Text fw={500} c={isDisabled ? 'dimmed' : c.textBright}>Турнирный оператор</Text>
                                <Text size="xs" c="dimmed">Разрешает создавать турниры</Text>
                            </div>
                        </Group>
                        <Switch
                            size="lg" color="orange"
                            disabled={isDisabled}
                            checked={isOperator}
                            onChange={e => setIsOperator(e.currentTarget.checked)}
                        />
                    </Group>
                </Stack>
                <Group justify="flex-end" mt="xl">
                    <Button
                        leftSection={<IconDeviceFloppy size={16} />}
                        color="green"
                        disabled={isDisabled}
                        onClick={handleSave}
                        loading={loading}
                    >
                        Сохранить
                    </Button>
                </Group>
            </Paper>
        </Container>
    );
}
