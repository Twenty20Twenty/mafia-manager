// src/pages/tournaments/TournamentProfilePage.jsx
import { useParams, Link } from 'react-router-dom';
import { Container, Button, Tabs, Center, Loader, Text, Box } from '@mantine/core';
import { IconArrowLeft, IconTable, IconSwords, IconUsers, IconStar } from '@tabler/icons-react';
import { useAuth } from '../../context/AuthContext';
import { useState, useEffect } from 'react';
import api from '../../api/axios';

import TournamentTable      from './TournamentTable';
import TournamentGamesTab   from './TournamentGamesTab';
import TournamentNominations from './TournamentNominations';
import TournamentHeader     from './components/profile/TournamentHeader';
import ParticipantsList     from './components/profile/ParticipantsList';
import { resolveCanManage } from './utils/tournamentUtils';

export default function TournamentProfilePage() {
    const { id }   = useParams();
    const { user } = useAuth();

    const [activeTab, setActiveTab]     = useState('table');
    const [tournament, setTournament]   = useState(null);
    const [participants, setParticipants] = useState([]);
    const [loading, setLoading]         = useState(true);
    const [error, setError]             = useState(null);

    useEffect(() => {
        if (!id) return;
        const fetchData = async () => {
            try {
                setLoading(true);
                const [tourRes, partRes] = await Promise.all([
                    api.get(`/tournaments/${id}`),
                    api.get(`/tournaments/${id}/participants`),
                ]);
                setTournament(tourRes.data);
                setParticipants(partRes.data);
            } catch (err) {
                console.error('Ошибка загрузки турнира', err);
                setError('Не удалось загрузить данные турнира');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [id]);

    const handleRegister = async () => {
        try {
            await api.post(`/tournaments/${id}/apply`);
            alert('Заявка на участие отправлена организатору!');
        } catch (error) {
            alert('Ошибка при подаче заявки: ' + (error.response?.data || error.message));
        }
    };

    if (loading) return <Center p="xl" mt="xl"><Loader color="brandRed" size="xl" /></Center>;
    if (error || !tournament) return <Container mt="xl"><Text c="red">{error || 'Турнир не найден'}</Text></Container>;

    const canManage            = resolveCanManage(user, tournament);
    const isRating             = tournament.type === 'season';
    const isTeamTournament     = tournament.type === 'team';
    const isRegistrationOpen   = tournament.status === 'registration';

    const approvedParticipants      = participants.filter(p => p.status === 'approved');
    const currentUserParticipation  = participants.find(p => p.id === user?.id);
    const isAlreadyParticipant      = currentUserParticipation?.status === 'approved';
    const isRequestSent             = currentUserParticipation?.status === 'pending';

    return (
        <Container size="xl" py="xl">
            <Button component={Link} to="/tournaments" variant="subtle" color="gray"
                    leftSection={<IconArrowLeft size={16} />} mb="md">
                Все турниры
            </Button>

            <TournamentHeader
                tournament={tournament}
                participantsCount={approvedParticipants.length}
                isRating={isRating}
                canManage={canManage}
                isRegistrationOpen={isRegistrationOpen}
                isAlreadyParticipant={isAlreadyParticipant}
                isRequestSent={isRequestSent}
                user={user}
                onRegister={handleRegister}
            />

            <Tabs value={activeTab} onChange={setActiveTab} variant="outline" radius="md">
                <Tabs.List mb="md" grow justify="flex-start">
                    <Tabs.Tab value="table" leftSection={<IconTable size={16} />}>
                        <Box style={{ textAlign: 'left' }}>
                            <Text visibleFrom="sm">Турнирная таблица</Text>
                            <Text hiddenFrom="sm">Таблица</Text>
                        </Box>
                    </Tabs.Tab>
                    <Tabs.Tab value="games" leftSection={<IconSwords size={16} />}>
                        <Box style={{ textAlign: 'left' }}>
                            <Text visibleFrom="sm">Игры (Туры)</Text>
                            <Text hiddenFrom="sm">Игры</Text>
                        </Box>
                    </Tabs.Tab>
                    {!isTeamTournament && (
                        <Tabs.Tab value="nominations" leftSection={<IconStar size={16} />}>
                            <Box style={{ textAlign: 'left' }}>
                                <Text visibleFrom="sm">Номинации</Text>
                                <Text hiddenFrom="sm">Номин.</Text>
                            </Box>
                        </Tabs.Tab>
                    )}
                    <Tabs.Tab value="participants" leftSection={<IconUsers size={16} />}>
                        <Box style={{ textAlign: 'left' }}>
                            <Text visibleFrom="sm">Участники</Text>
                            <Text hiddenFrom="sm">Игроки</Text>
                        </Box>
                    </Tabs.Tab>
                </Tabs.List>

                <Tabs.Panel value="table">
                    <TournamentTable tournament={tournament} />
                </Tabs.Panel>
                <Tabs.Panel value="games">
                    <TournamentGamesTab tournament={tournament} participantOptions={approvedParticipants} />
                </Tabs.Panel>
                {!isTeamTournament && (
                    <Tabs.Panel value="nominations">
                        <TournamentNominations tournament={tournament} isRating={isRating} />
                    </Tabs.Panel>
                )}
                <Tabs.Panel value="participants">
                    <ParticipantsList participants={approvedParticipants} isTeamTournament={isTeamTournament} />
                </Tabs.Panel>
            </Tabs>
        </Container>
    );
}
