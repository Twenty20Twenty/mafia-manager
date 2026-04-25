// src/pages/judges/ManageJudgesPage.jsx
import { useState, useEffect } from 'react';
import { Container, Title, Paper, Group, Button, Select, Avatar, Text, Switch, Stack, Divider, Alert, Box } from '@mantine/core';
import {
    IconArrowLeft, IconSearch, IconDeviceFloppy, IconUserCheck, IconCertificate,
    IconGavel, IconShieldLock
} from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import api from '../../api/axios';
import { useThemeColors } from '../../hooks/useThemeColors';

export default function ManageJudgesPage() {
    const { user } = useAuth();
    const c = useThemeColors();

    const [searchValue, setSearchValue]           = useState('');
    const [usersList, setUsersList]               = useState([]);
    const [judgesMap, setJudgesMap]               = useState({});
    const [selectedPlayerId, setSelectedPlayerId] = useState(null);
    const [permissions, setPermissions]           = useState({ isJudge: false, canJudgeFinals: false, canBeHeadJudge: false });
    const [loading, setLoading]                   = useState(false);
    const [saving, setSaving]                     = useState(false);

    useEffect(() => {
        if (user?.isAdmin) {
            api.get('/judges', { params: { size: 1000 } })
                .then(res => {
                    const map = {};
                    res.data.content.forEach(j => { map[j.userId] = j; });
                    setJudgesMap(map);
                })
                .catch(console.error);
        }
    }, [user]);

    useEffect(() => {
        if (!user?.isAdmin) return;
        const timer = setTimeout(() => {
            api.get('/users', { params: { search: searchValue, size: 20 } })
                .then(res => setUsersList(res.data.content))
                .catch(console.error);
        }, 300);
        return () => clearTimeout(timer);
    }, [searchValue, user]);

    if (!user?.isAdmin) {
        return (
            <Container py="xl">
                <Alert color="red" icon={<IconShieldLock />}>Доступ запрещен. Только для администраторов.</Alert>
            </Container>
        );
    }

    const selectData = usersList.map(p => {
        const judgeInfo = judgesMap[p.id] || {};
        return {
            value:          String(p.id),
            label:          p.nickname,
            avatar:         p.avatarUrl,
            isJudge:        judgeInfo.isJudge        || false,
            canJudgeFinals: judgeInfo.canJudgeFinals || false,
            canBeHeadJudge: judgeInfo.canBeHeadJudge || false,
        };
    });

    const renderSelectOption = ({ option }) => (
        <Group justify="space-between" w="100%" wrap="nowrap">
            <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
                <Avatar src={option.avatar} size={24} radius="xl" color="brandRed" style={{ flexShrink: 0 }}>
                    {option.label?.[0]}
                </Avatar>
                <Text size="sm" truncate>{option.label}</Text>
            </Group>
            <Group gap={6} style={{ flexShrink: 0 }}>
                {option.canBeHeadJudge && <IconGavel       size={16} color="var(--mantine-color-violet-4)" />}
                {option.canJudgeFinals && <IconCertificate size={16} color="var(--mantine-color-orange-4)" />}
                {option.isJudge && !option.canBeHeadJudge && !option.canJudgeFinals && (
                    <IconUserCheck size={16} color="var(--mantine-color-gray-5)" />
                )}
            </Group>
        </Group>
    );

    const handleSelectUser = async (value) => {
        setSelectedPlayerId(value);
        if (!value) {
            setPermissions({ isJudge: false, canJudgeFinals: false, canBeHeadJudge: false });
            return;
        }
        setLoading(true);
        try {
            const res = await api.get(`/judges/${value}`);
            setPermissions({
                isJudge:        res.data.isJudge        || false,
                canJudgeFinals: res.data.canJudgeFinals || false,
                canBeHeadJudge: res.data.canBeHeadJudge || false,
            });
        } catch {
            setPermissions({ isJudge: false, canJudgeFinals: false, canBeHeadJudge: false });
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            const res = await api.put(`/admin/judges/${selectedPlayerId}`, permissions);
            alert('Права для судьи успешно обновлены!');
            setJudgesMap(prev => ({ ...prev, [selectedPlayerId]: res.data }));
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка при сохранении прав');
        } finally {
            setSaving(false);
        }
    };

    const isControlsDisabled  = !selectedPlayerId || loading;
    const isSubStatusDisabled = isControlsDisabled || !permissions.isJudge;

    const SwitchRow = ({ icon, label, description, color, disabled, checked, onChange }) => (
        <Group
            justify="space-between" p="sm"
            style={{
                borderRadius: 8,
                backgroundColor: disabled ? 'transparent' : c.surface3,
            }}
            wrap="nowrap"
        >
            <Group wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
                <Box style={{ flexShrink: 0 }}>{icon}</Box>
                <div style={{ minWidth: 0 }}>
                    <Text fw={500} c={disabled ? 'dimmed' : c.textBright} size="sm">{label}</Text>
                    <Text size="xs" c="dimmed">{description}</Text>
                </div>
            </Group>
            <Switch size="md" color={color} disabled={disabled} checked={checked} onChange={onChange} style={{ flexShrink: 0 }} />
        </Group>
    );

    return (
        <Container size="sm" py="xl">
            <Button component={Link} to="/judges" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16} />} mb="md">
                Назад к списку
            </Button>
            <Title order={2} mb="xl">Выдача статуса судьи</Title>

            <Paper withBorder p={{ base: 'md', sm: 'xl' }} radius="md" style={{ backgroundColor: c.surface2 }}>
                <Group align="flex-end" mb="xl">
                    <Select
                        label="Поиск игрока"
                        description="Выберите игрока, чтобы изменить его права"
                        placeholder="Введите никнейм..."
                        data={selectData}
                        renderOption={renderSelectOption}
                        searchable
                        searchValue={searchValue}
                        onSearchChange={setSearchValue}
                        filter={({ options }) => options}
                        nothingFoundMessage="Игрок не найден"
                        leftSection={<IconSearch size={16} />}
                        size="md"
                        style={{ flex: 1 }}
                        onChange={handleSelectUser}
                        value={selectedPlayerId}
                    />
                </Group>

                <Divider mb="xl" label="Настройка прав" labelPosition="center" />

                <Stack gap="lg">
                    <SwitchRow
                        icon={<IconUserCheck size={20} color={isControlsDisabled ? 'gray' : 'var(--mantine-color-brandRed-5)'} />}
                        label="Статус Судьи"
                        description="Базовое право судить игры"
                        color="brandRed"
                        disabled={isControlsDisabled}
                        checked={permissions.isJudge}
                        onChange={e => {
                            const checked = e.currentTarget.checked;
                            setPermissions({
                                isJudge:        checked,
                                canJudgeFinals: checked ? permissions.canJudgeFinals : false,
                                canBeHeadJudge: checked ? permissions.canBeHeadJudge : false,
                            });
                        }}
                    />
                    <SwitchRow
                        icon={<IconCertificate size={20} color={isSubStatusDisabled ? 'gray' : 'var(--mantine-color-orange-5)'} />}
                        label="Судейство финалов"
                        description="Допуск к финальным играм турниров"
                        color="orange"
                        disabled={isSubStatusDisabled}
                        checked={permissions.canJudgeFinals}
                        onChange={e => setPermissions({ ...permissions, canJudgeFinals: e.currentTarget.checked })}
                    />
                    <SwitchRow
                        icon={<IconGavel size={20} color={isSubStatusDisabled ? 'gray' : 'var(--mantine-color-violet-5)'} />}
                        label="Главный Судья (ГС)"
                        description="Может организовывать турниры"
                        color="violet"
                        disabled={isSubStatusDisabled}
                        checked={permissions.canBeHeadJudge}
                        onChange={e => setPermissions({ ...permissions, canBeHeadJudge: e.currentTarget.checked })}
                    />
                </Stack>

                <Group justify="flex-end" mt="xl" wrap="wrap">
                    <Button
                        variant="default"
                        disabled={isControlsDisabled}
                        onClick={() => {
                            setSelectedPlayerId(null);
                            setPermissions({ isJudge: false, canJudgeFinals: false, canBeHeadJudge: false });
                        }}
                    >
                        Сброс
                    </Button>
                    <Button
                        leftSection={<IconDeviceFloppy size={16} />}
                        color="green"
                        disabled={isControlsDisabled}
                        onClick={handleSave}
                        loading={saving}
                    >
                        Сохранить
                    </Button>
                </Group>
            </Paper>
        </Container>
    );
}
