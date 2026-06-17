// src/pages/tournaments/TournamentsPage.jsx
import { useState, useEffect, useCallback, useRef } from 'react';
import { Container, Title, Group, Text, Select, TextInput, Stack, Center, Loader, Box, Pagination } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import api from '../../api/axios';
import TournamentCard from './components/TournamentCard';
import { DEBOUNCE_MS } from './constants/tournamentConstants';
import { sortTournaments } from './utils/tournamentSortUtils';

const PAGE_SIZE = 10;

export default function TournamentsPage() {
    const [search, setSearch]             = useState('');
    const [typeFilter, setTypeFilter]     = useState('all');
    const [statusFilter, setStatusFilter] = useState('all');
    const [tournaments, setTournaments]   = useState([]);
    const [loading, setLoading]           = useState(true);
    const [activePage, setActivePage]     = useState(1);

    const debounceRef = useRef(null);

    const fetchTournaments = useCallback(async (searchValue, type, status) => {
        try {
            setLoading(true);
            const params = {};
            if (type   && type   !== 'all') params.type   = type;
            if (status && status !== 'all') params.status = status;
            if (searchValue?.trim())        params.search = searchValue.trim();

            const res = await api.get('/tournaments', { params });
            setTournaments(sortTournaments(res.data));
            setActivePage(1);
        } catch (error) {
            console.error('Ошибка загрузки турниров', error);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchTournaments(search, typeFilter, statusFilter);
    }, [typeFilter, statusFilter]); // eslint-disable-line

    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        debounceRef.current = setTimeout(() => {
            fetchTournaments(search, typeFilter, statusFilter);
        }, DEBOUNCE_MS);
        return () => clearTimeout(debounceRef.current);
    }, [search]); // eslint-disable-line

    const totalPages = Math.ceil(tournaments.length / PAGE_SIZE);
    const paginated  = tournaments.slice((activePage - 1) * PAGE_SIZE, activePage * PAGE_SIZE);

    return (
        <Container size="xl" py="xl">
            {/* Десктоп */}
            <Group justify="space-between" mb="xl" align="flex-start">
                <Title order={2}>Турниры и Рейтинги</Title>
                <Group visibleFrom="sm">
                    <Select
                        placeholder="Тип турнира"
                        data={TYPE_OPTIONS}
                        value={typeFilter}
                        onChange={v => setTypeFilter(v || 'all')}
                        w={170}
                    />
                    <Select
                        placeholder="Статус"
                        data={STATUS_OPTIONS}
                        value={statusFilter}
                        onChange={v => setStatusFilter(v || 'all')}
                        w={165}
                    />
                    <TextInput
                        placeholder="Поиск по названию..."
                        leftSection={<IconSearch size={16} />}
                        value={search}
                        onChange={e => setSearch(e.currentTarget.value)}
                        w={240}
                    />
                </Group>
            </Group>

            {/* Мобилка */}
            <Box hiddenFrom="sm" mb="md" mx="-20px">
                <Stack gap="xs">
                    <TextInput
                        placeholder="Поиск по названию..."
                        leftSection={<IconSearch size={16} />}
                        value={search}
                        onChange={e => setSearch(e.currentTarget.value)}
                    />
                    <Group grow gap="xs">
                        <Select
                            placeholder="Тип"
                            data={TYPE_OPTIONS}
                            value={typeFilter}
                            onChange={v => setTypeFilter(v || 'all')}
                        />
                        <Select
                            placeholder="Статус"
                            data={STATUS_OPTIONS}
                            value={statusFilter}
                            onChange={v => setStatusFilter(v || 'all')}
                        />
                    </Group>
                </Stack>
            </Box>

            {loading ? (
                <Center py="xl"><Loader color="brandRed" /></Center>
            ) : (
                <Box mx={{ base: '-20px', sm: 0 }}>
                    {tournaments.length === 0 ? (
                        <Text c="dimmed" ta="center" py="xl">Турниры не найдены</Text>
                    ) : (
                        <Stack gap="md">
                            {paginated.map(tour => <TournamentCard key={tour.id} tour={tour} />)}
                        </Stack>
                    )}

                    {totalPages > 1 && (
                        <Center mt="xl">
                            <Pagination
                                total={totalPages}
                                value={activePage}
                                onChange={setActivePage}
                                color="brandRed"
                                size={{ base: 'sm', sm: 'md' }}
                            />
                        </Center>
                    )}
                </Box>
            )}
        </Container>
    );
}

// ── Константы для Select'ов ───────────────────────────────────────────────────

const TYPE_OPTIONS = [
    { value: 'all',        label: 'Все типы'  },
    { value: 'individual', label: 'Личные'    },
    { value: 'team',       label: 'Командные' },
    { value: 'season',     label: 'Рейтинги'  },
];

const STATUS_OPTIONS = [
    { value: 'all',          label: 'Все статусы' },
    { value: 'registration', label: 'Регистрация' },
    { value: 'active',       label: 'Активные'    },
    { value: 'completed',    label: 'Завершённые' },
];