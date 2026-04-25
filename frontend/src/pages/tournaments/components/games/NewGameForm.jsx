// src/pages/tournaments/components/games/NewGameForm.jsx
import { useState, useEffect } from 'react';
import { Container, Title, Paper, Stack, Select, Button, Text } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconArrowLeft, IconCalendar, IconGavel, IconPlayerPlay } from '@tabler/icons-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../../context/AuthContext';
import { gamesApi } from '../../../../api/games';
import api from '../../../../api/axios';

export default function NewGameForm({ tournamentId, onGameCreated }) {
    const { user } = useAuth();

    const [date, setDate]               = useState(new Date());
    const [judgeId, setJudgeId]         = useState(user?.id ? String(user.id) : null);
    const [judgeOptions, setJudgeOptions] = useState([]);
    const [creating, setCreating]       = useState(false);
    const [loadingJudges, setLoadingJudges] = useState(true);

    useEffect(() => {
        api.get('/judges?size=200')
            .then(res => {
                const list = res.data?.content ?? res.data ?? [];
                setJudgeOptions(list.map(j => ({ value: String(j.userId), label: j.nickname })));
            })
            .catch(console.error)
            .finally(() => setLoadingJudges(false));
    }, []);

    const handleCreate = async () => {
        if (!date)    { alert('Выберите дату игры'); return; }
        if (!judgeId) { alert('Выберите судью');     return; }

        setCreating(true);
        try {
            const dto = await gamesApi.createGame(tournamentId, {
                date:    date instanceof Date ? date.toISOString().split('T')[0] : date,
                judgeId: Number(judgeId),
            });
            onGameCreated(dto);
        } catch (err) {
            alert('Ошибка создания игры: ' + (err.response?.data?.message || err.message));
        } finally {
            setCreating(false);
        }
    };

    return (
        <Container size="sm" py="xl">
            <Button
                component={Link}
                to={`/tournaments/${tournamentId}`}
                variant="subtle" color="gray"
                leftSection={<IconArrowLeft size={16} />}
                mb="xl"
            >
                Назад к турниру
            </Button>

            <Title order={3} mb="xl">Новая игра (Рейтинг)</Title>

            <Paper withBorder p="xl" radius="md">
                <Stack gap="md">
                    <DatePickerInput
                        label="Дата проведения"
                        placeholder="Выберите дату"
                        leftSection={<IconCalendar size={16} />}
                        value={date}
                        onChange={setDate}
                        required
                        maxDate={new Date()}
                    />
                    <Select
                        label="Судья стола"
                        placeholder={loadingJudges ? 'Загрузка...' : 'Выберите судью'}
                        leftSection={<IconGavel size={16} />}
                        data={judgeOptions}
                        value={judgeId}
                        onChange={setJudgeId}
                        searchable required
                        disabled={loadingJudges}
                        nothingFoundMessage="Судья не найден"
                    />
                    <Text size="sm" c="dimmed">
                        После создания игры откроется форма заполнения протокола.
                    </Text>
                    <Button
                        leftSection={<IconPlayerPlay size={18} />}
                        color="green" size="md"
                        onClick={handleCreate}
                        loading={creating}
                        fullWidth
                    >
                        Создать игру и заполнить протокол
                    </Button>
                </Stack>
            </Paper>
        </Container>
    );
}
