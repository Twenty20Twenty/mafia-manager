// src/pages/tournaments/components/profile/RatingThresholdInfo.jsx
import { Box, Group, Text, ThemeIcon, Tooltip } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconChartBar, IconInfoCircle } from '@tabler/icons-react';
import { calcThresholdGames } from '../../utils/tournamentRatingUtils';

export default function RatingThresholdInfo({ tournament }) {
    const threshold = tournament.settings?.ratingThreshold ?? 0;
    const totalGames = tournament.completedGamesCount ?? 0;
    const thresholdGames = calcThresholdGames(totalGames, threshold);

    const [opened, { toggle, close }] = useDisclosure(false);

    return (
        <Group gap="xs" wrap="wrap" align="center">
            <ThemeIcon size="sm" variant="light" color="teal">
                <IconChartBar size={14} />
            </ThemeIcon>

            <Group gap="xs" wrap="nowrap" align="center">
                <Text size="sm" c="dimmed">Игр сыграно:</Text>
                <Text size="sm" fw={600}>{totalGames}</Text>
            </Group>

            <Group gap="xs" wrap="nowrap" align="center">
                <Text size="sm" c="dimmed">Порог:</Text>
                <Text size="sm" fw={600}>{threshold}%</Text>

                <Tooltip
                    label={`Минимум ${thresholdGames} игр для попадания в рейтинг`}
                    withArrow
                    opened={opened}
                    events={{ hover: true, focus: true, touch: false }}
                >
                    <Box
                        onClick={toggle}
                        onMouseLeave={close}
                        style={{ cursor: 'pointer' }}
                    >
                        <Group gap={4} wrap="nowrap" align="center">
                            <Text size="sm" fw={600} c="teal">{thresholdGames} игр</Text>
                            <IconInfoCircle size={14} color="gray" />
                        </Group>
                    </Box>
                </Tooltip>
            </Group>
        </Group>
    );
}