// src/pages/tournaments/TournamentGamesTab.jsx
import { Stack, Paper, Text, Center, Loader } from '@mantine/core';
import { useAuth } from '../../context/AuthContext';
import { useState, useEffect, useCallback } from 'react';
import api from '../../api/axios';
import { resolveCanManage } from './utils/tournamentUtils';
import RatingGamesFeed from './components/games/RatingGamesFeed';
import RoundBlock      from './components/games/RoundBlock';

export default function TournamentGamesTab({ tournament, participantOptions }) {
    const { user } = useAuth();
    const [games,   setGames]   = useState([]);
    const [loading, setLoading] = useState(true);

    const canManage = resolveCanManage(user, tournament);
    const isRating  = tournament?.type === 'season';

    const fetchGames = useCallback(async () => {
        if (!tournament?.id) return;
        try {
            setLoading(true);
            const res = await api.get(`/tournaments/${tournament.id}/games`);
            setGames(res.data);
        } catch (error) {
            console.error('Ошибка загрузки игр', error);
        } finally {
            setLoading(false);
        }
    }, [tournament?.id]);

    useEffect(() => { fetchGames(); }, [fetchGames]);

    if (loading) return <Center p="xl"><Loader color="brandRed" /></Center>;

    if (isRating) {
        return <RatingGamesFeed tournament={tournament} canManage={canManage} user={user} games={games} onRefresh={fetchGames} />;
    }

    if (!tournament?.settings?.isSeedingGenerated && games.length === 0) {
        return (
            <Paper withBorder p="xl" ta="center" bg="transparent" style={{ borderStyle: 'dashed' }}>
                <Text c="dimmed">Рассадка еще не сформирована организатором.</Text>
            </Paper>
        );
    }

    if (games.length === 0) {
        return (
            <Paper withBorder p="xl" ta="center" bg="var(--mantine-color-dark-6)">
                <Text c="dimmed">Игры еще не сгенерированы.</Text>
            </Paper>
        );
    }

    const groupedByRound = games.reduce((acc, game) => {
        const key = game.round ?? 0;
        if (!acc[key]) acc[key] = [];
        acc[key].push(game);
        return acc;
    }, {});

    const sortedRounds = Object.keys(groupedByRound).sort((a, b) => Number(a) - Number(b));

    return (
        <Stack gap="xl">
            {sortedRounds.map(roundNum => (
                <RoundBlock
                    key={roundNum}
                    roundNum={roundNum}
                    tables={groupedByRound[roundNum]}
                    tournament={tournament}
                    user={user}
                    participantOptions={participantOptions}
                    onRefresh={fetchGames}
                />
            ))}
        </Stack>
    );
}
