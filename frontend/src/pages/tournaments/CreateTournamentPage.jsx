// src/pages/tournaments/CreateTournamentPage.jsx
import {
    Container, Title, TextInput, Button, Paper, Group,
    Select, NumberInput, Alert, Center, Loader
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconArrowLeft, IconCalendar, IconShieldLock } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/axios';
import dayjs from 'dayjs';

export default function CreateTournamentPage() {
    const navigate = useNavigate();
    const { user } = useAuth();

    const [judgesData,    setJudgesData]    = useState([]);
    const [citiesData,    setCitiesData]    = useState([]);
    const [loading,       setLoading]       = useState(false);
    const [fullUser,      setFullUser]      = useState(null);
    const [isUserLoading, setIsUserLoading] = useState(true);

    const [formData, setFormData] = useState({
        title:           '',
        type:            'individual',
        maxParticipants: 10,
        dates:           [null, null],
        headJudgeId:     null,
        cityId:          null,
    });

    useEffect(() => {
        if (!user) { setIsUserLoading(false); return; }

        const fetchInitialData = async () => {
            try {
                const [userRes, judgesRes, citiesRes] = await Promise.all([
                    api.get(`/users/${user.id}`),
                    api.get('/judges', { params: { canBeHead: true, size: 100 } }),
                    api.get('/geo/cities'),
                ]);
                setFullUser(userRes.data);
                setJudgesData(
                    judgesRes.data.content.map(p => ({ value: String(p.userId), label: p.nickname }))
                );
                setCitiesData(
                    citiesRes.data.map(c => ({ value: String(c.id), label: c.name }))
                );
            } catch (error) {
                console.error('Ошибка загрузки данных', error);
            } finally {
                setIsUserLoading(false);
            }
        };
        fetchInitialData();
    }, [user]);

    if (isUserLoading) return <Center py="xl"><Loader color="brandRed" /></Center>;
    if (!user || !fullUser) return <Container py="xl">Пожалуйста, войдите в систему</Container>;

    const isAdmin   = fullUser.role === 'admin';
    const canCreate = isAdmin || fullUser.canCreateTournaments;

    if (!canCreate) {
        return (
            <Container py="xl">
                <Alert color="red" icon={<IconShieldLock />}>
                    У вас нет лицензии на создание турниров.
                </Alert>
            </Container>
        );
    }

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        const payload = {
            title:       formData.title,
            description: '',
            type:        formData.type,
            startDate:   formData.dates[0] ? dayjs(formData.dates[0]).format('YYYY-MM-DD') : null,
            endDate:     formData.dates[1] ? dayjs(formData.dates[1]).format('YYYY-MM-DD') : null,
            cityId:      formData.cityId ? Number(formData.cityId) : null,
            settings: {
                maxParticipants:    formData.maxParticipants,
                roundsCount:        10,
                finalRoundsCount:   0,
                ratingThreshold:    formData.type === 'season' ? 40 : 0,
                isSeedingGenerated: false,
                areResultsHidden:   false,
            },
        };
        if (formData.headJudgeId) payload.headJudgeId = Number(formData.headJudgeId);

        try {
            const res = await api.post('/tournaments', payload);
            alert('Турнир успешно создан!');
            navigate(`/tournaments/${res.data.id}`);
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка создания турнира.');
        } finally {
            setLoading(false);
        }
    };

    const set = (field, val) => setFormData(prev => ({ ...prev, [field]: val }));

    return (
        <Container size="sm" py="xl">
            <Button
                component={Link} to="/tournaments"
                variant="subtle" color="gray"
                leftSection={<IconArrowLeft size={16} />}
                mb="md"
            >
                Отмена
            </Button>

            <Title order={2} mb="lg">Создание турнира</Title>

            <Paper withBorder p="xl" radius="md">
                <form onSubmit={handleSubmit}>
                    <TextInput
                        label="Название турнира"
                        placeholder="Например: Кубок Сибири"
                        required mb="md"
                        value={formData.title}
                        onChange={e => set('title', e.currentTarget.value)}
                    />

                    <Group grow mb="md">
                        <Select
                            label="Тип турнира"
                            data={[
                                { value: 'individual', label: 'Личный зачёт'    },
                                { value: 'team',       label: 'Командный зачёт' },
                                { value: 'season',     label: 'Рейтинг'         },
                            ]}
                            value={formData.type}
                            onChange={val => set('type', val)}
                        />
                        {formData.type !== 'season' && (
                            <NumberInput
                                label="Количество участников"
                                defaultValue={10} min={10} step={10}
                                value={formData.maxParticipants}
                                onChange={val => set('maxParticipants', val)}
                            />
                        )}
                    </Group>

                    <Select
                        label="Город проведения"
                        data={citiesData}
                        searchable clearable
                        placeholder="Выберите город"
                        value={formData.cityId}
                        onChange={val => set('cityId', val)}
                        mb="md"
                    />

                    <DatePickerInput
                        type="range"
                        label="Даты проведения"
                        placeholder="Выберите период"
                        leftSection={<IconCalendar size={16} />}
                        value={formData.dates}
                        onChange={val => set('dates', val)}
                        mb="md"
                    />

                    <Select
                        label="Главный судья (необязательно)"
                        data={judgesData}
                        searchable clearable
                        placeholder="Выберите судью"
                        value={formData.headJudgeId}
                        onChange={val => set('headJudgeId', val)}
                        mb="xl"
                    />

                    <Button type="submit" color="brandRed" fullWidth loading={loading}>
                        Создать турнир
                    </Button>
                </form>
            </Paper>
        </Container>
    );
}
