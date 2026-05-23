// src/pages/tournaments/components/management/SettingsTab.jsx
import { Stack, Button, Select, Paper, Title, Text, Avatar, Group, TextInput, Modal } from '@mantine/core';
import { IconDeviceFloppy, IconTrash, IconAlertTriangle } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../../api/axios';
import SeasonSettingsForm     from './settings/SeasonSettingsForm';
import IndividualSettingsForm from './settings/IndividualSettingsForm';
import TeamSettingsForm       from './settings/TeamSettingsForm';

function JudgeOption({ option }) {
    return (
        <Group gap="sm" w="100%">
            <Avatar src={option.avatar} size="xs" radius="xl" />
            <Text size="sm">{option.label}</Text>
        </Group>
    );
}

function DeleteTournamentModal({ opened, onClose, tournament }) {
    const navigate = useNavigate();
    const [confirmTitle, setConfirmTitle] = useState('');
    const [loading, setLoading]           = useState(false);
    const [error, setError]               = useState(null);

    // Сбрасываем поле при каждом открытии
    useEffect(() => {
        if (opened) {
            setConfirmTitle('');
            setError(null);
        }
    }, [opened]);

    const handleDelete = async () => {
        if (!tournament) return;
        setLoading(true);
        setError(null);
        try {
            await api.delete(`/tournaments/${tournament.id}`, {
                params: { confirmTitle: confirmTitle.trim() },
            });
            onClose();
            navigate('/tournaments');
        } catch (err) {
            setError(err.response?.data?.message || err.response?.data || 'Ошибка при удалении');
        } finally {
            setLoading(false);
        }
    };

    // Сравниваем с trim() с обеих сторон
    const isMatch = tournament?.title && confirmTitle.trim() === tournament.title.trim();
    //console.log(tournament?.title)

    return (
        <Modal
            opened={opened}
            onClose={onClose}
            title="Удаление турнира"
            centered
            size="sm"
        >
            <Stack gap="md">
                <Group gap="xs" wrap="nowrap">
                    <IconAlertTriangle size={20} color="var(--mantine-color-red-5)" style={{ flexShrink: 0 }} />
                    <Text size="sm" fw={500}>Это действие необратимо.</Text>
                </Group>

                <Text size="sm" c="dimmed">
                    Будут каскадно удалены все игры, участники, исключения рассадки и настройки турнира.
                </Text>

                <TextInput
                    label="Введите название турнира для подтверждения"
                    description={
                        <Text size="xs" c="dimmed">
                            Введите точно: <Text span fw={700} c="red">{tournament?.title}</Text>
                        </Text>
                    }
                    placeholder={tournament?.title ?? ''}
                    value={confirmTitle}
                    onChange={e => setConfirmTitle(e.currentTarget.value)}
                    error={error}
                    data-autofocus
                />

                {/* Подсказка о совпадении */}
                {confirmTitle.length > 0 && !isMatch && (
                    <Text size="xs" c="dimmed">
                        Введено: «{confirmTitle}» — не совпадает с названием турнира
                    </Text>
                )}

                <Group justify="flex-end">
                    <Button variant="subtle" color="gray" onClick={onClose} disabled={loading}>
                        Отмена
                    </Button>
                    <Button
                        color="red"
                        leftSection={<IconTrash size={16} />}
                        disabled={!isMatch}
                        loading={loading}
                        onClick={handleDelete}
                    >
                        Удалить турнир
                    </Button>
                </Group>
            </Stack>
        </Modal>
    );
}

export default function SettingsTab({
                                        settings, setSettings,
                                        isRating, isTeam,
                                        allJudgesOptions,
                                        headJudgeId, setHeadJudgeId,
                                        onSave,
                                        tournament,
                                        canDelete,
                                    }) {
    const [citiesData, setCitiesData] = useState([]);
    const [deleteOpen, setDeleteOpen] = useState(false);

    useEffect(() => {
        api.get('/geo/cities')
            .then(res => setCitiesData(res.data.map(c => ({ value: String(c.id), label: c.name }))))
            .catch(console.error);
    }, []);

    const renderOption = ({ option }) => <JudgeOption option={option} />;

    const SettingsForm = isRating ? SeasonSettingsForm
        : isTeam          ? TeamSettingsForm
            :                   IndividualSettingsForm;

    return (
        <Stack gap="lg">
            {/* Главный судья — для всех типов */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Главный судья</Title>
                <Select
                    label="Главный судья турнира"
                    description="Имеет доступ ко всем настройкам и протоколам"
                    data={allJudgesOptions}
                    renderOption={renderOption}
                    searchable clearable
                    value={headJudgeId || null}
                    onChange={val => setHeadJudgeId(val || '')}
                />
            </Paper>

            {/* Форма настроек по типу турнира */}
            <SettingsForm
                settings={settings}
                setSettings={setSettings}
                citiesData={citiesData}
            />

            <Button
                leftSection={<IconDeviceFloppy size={16} />}
                color="brandRed"
                onClick={onSave}
            >
                Сохранить настройки
            </Button>

            {/* Удаление турнира */}
            {canDelete && (
                <Paper withBorder p="md" style={{ borderColor: 'var(--mantine-color-red-7)' }}>
                    <Title order={4} mb="xs" c="red">Опасная зона</Title>
                    <Text size="sm" c="dimmed" mb="md">
                        Удаление турнира приведёт к безвозвратному удалению всех игр, участников и результатов.
                    </Text>
                    <Button
                        color="red"
                        variant="outline"
                        leftSection={<IconTrash size={16} />}
                        onClick={() => setDeleteOpen(true)}
                    >
                        Удалить турнир
                    </Button>
                </Paper>
            )}

            <DeleteTournamentModal
                opened={deleteOpen}
                onClose={() => setDeleteOpen(false)}
                tournament={tournament}
            />
        </Stack>
    );
}
