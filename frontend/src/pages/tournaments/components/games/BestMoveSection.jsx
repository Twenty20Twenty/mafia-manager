// src/pages/tournaments/components/games/BestMoveSection.jsx
import { Paper, Text, Grid, Select } from '@mantine/core';

export default function BestMoveSection({ bestMove, setBestMove, slots }) {
    const authorOptions = slots
        .filter(s => s.isFirstKilled && s.playerId)
        .map(s => ({ value: String(s.slotNumber), label: `#${s.slotNumber} ${s.playerNickname || s.playerId || s.nickname}` }));

    const candidateOptions = slots
        .filter(s => s.playerId)
        .map(s => ({ value: String(s.slotNumber), label: `#${s.slotNumber} ${s.playerNickname || s.playerId}` }));

    if (authorOptions.length === 0) return null;

    const authorValue    = bestMove.authorSlotNumber != null ? String(bestMove.authorSlotNumber) : null;
    const candidateValues = bestMove.candidates.map(c => (c != null ? String(c) : null));



    return (
        <Paper p="md" withBorder radius="md" mb="xl">
            <Text fw={700} mb="md">Лучший Ход (ЛХ)</Text>
            <Grid>
                <Grid.Col span={{ base: 12, sm: 4 }}>
                    <Select
                        label="Автор ЛХ (ПУ)"
                        data={authorOptions}
                        value={authorValue}
                        onChange={v => setBestMove(bm => ({
                            ...bm,
                            authorSlotNumber: v != null ? Number(v) : null,
                            candidates: [null, null, null],
                            points: 0,
                        }))}
                        clearable
                    />
                </Grid.Col>
                {[0, 1, 2].map(i => (
                    <Grid.Col key={i} span={{ base: 12, sm: 2.5 }}>
                        <Select
                            label={`Кандидат ${i + 1}`}
                            data={candidateOptions}
                            value={candidateValues[i]}
                            onChange={v => setBestMove(bm => {
                                const cands = [...bm.candidates];
                                cands[i] = v != null ? Number(v) : null;
                                return { ...bm, candidates: cands };
                            })}
                            clearable
                            disabled={!bestMove.authorSlotNumber}
                        />
                    </Grid.Col>
                ))}
                <Grid.Col span={{ base: 12, sm: 1.5 }}>
                    <Text size="sm" c="dimmed" mt="xl">
                        Баллы: <b>{bestMove.points.toFixed(2)}</b>
                    </Text>
                </Grid.Col>
            </Grid>
        </Paper>
    );
}
