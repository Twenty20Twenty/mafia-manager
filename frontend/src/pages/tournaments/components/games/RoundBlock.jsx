import { useMemo } from 'react'; // Добавляем импорт useMemo
import { Paper, Title, Group, ScrollArea } from '@mantine/core';
import GameTableCard from './GameTableCard';
import { useThemeColors } from '../../../../hooks/useThemeColors';

export default function RoundBlock({ roundNum, tables, tournament, user, participantOptions, onRefresh }) {
    const c = useThemeColors();

    // Сортируем столы по возрастанию (1, 2, 3...)
    const sortedTables = useMemo(() => {
        if (!tables) return [];
        // Создаем копию через [...tables], так как .sort() мутирует исходный массив
        return [...tables].sort((a, b) => a.table - b.table);
    }, [tables]);

    return (
        <Paper withBorder p="md" radius="md" style={{ backgroundColor: c.surface0 }}>
            <Title order={4} mb="md" c="dimmed">Тур {roundNum}</Title>
            <ScrollArea type="auto" offsetScrollbars>
                <Group align="stretch" wrap="nowrap" pb="lg">
                    {/* Используем отсортированный массив вместо tables */}
                    {sortedTables.map(game => (
                        <GameTableCard
                            key={game.id}
                            game={game}
                            tournament={tournament}
                            user={user}
                            participantOptions={participantOptions}
                            onRefresh={onRefresh}
                            isRating={false}
                        />
                    ))}
                </Group>
            </ScrollArea>
        </Paper>
    );
}