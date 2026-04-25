// src/pages/tournaments/components/profile/RatingThresholdInfo.jsx
import { Group, Text, ThemeIcon, Tooltip } from '@mantine/core';
import { IconChartBar, IconInfoCircle } from '@tabler/icons-react';
import { calcThresholdGames } from '../../utils/tournamentRatingUtils';

export default function RatingThresholdInfo({ tournament }) {
    const threshold = tournament.settings?.ratingThreshold ?? 0;
    const totalGames = tournament.completedGamesCount ?? 0;
    const thresholdGames = calcThresholdGames(totalGames, threshold);

    return (
        <Group gap="xs" wrap="nowrap">
            <ThemeIcon size="sm" variant="light" color="teal">
                <IconChartBar size={14} />
            </ThemeIcon>
            <Text size="sm" c="dimmed">Игр сыграно:</Text>
            <Text size="sm" fw={600}>{totalGames}</Text>

            <Text size="sm" c="dimmed" ml="sm">Порог:</Text>
            <Text size="sm" fw={600}>{threshold}%</Text>

            <Tooltip
                label={`Минимум ${thresholdGames} игр для попадания в рейтинг`}
                withArrow
            >
                <Group gap={4} style={{ cursor: 'default' }}>
                    <Text size="sm" fw={600} c="teal">{thresholdGames} игр</Text>
                    <IconInfoCircle size={14} color="gray" />
                </Group>
            </Tooltip>
        </Group>
    );
}