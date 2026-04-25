// src/pages/tournaments/components/management/JudgesTab.jsx
import { Stack, Paper, Title, Group, Select, MultiSelect, Text, Avatar, Table, Button } from '@mantine/core';
import { IconDeviceFloppy } from '@tabler/icons-react';
import { useThemeColors } from '../../../../hooks/useThemeColors';

function JudgeOption({ option }) {
    return (
        <Group gap="sm" w="100%">
            <Avatar src={option.avatar} size="xs" radius="xl" />
            <Text size="sm">{option.label}</Text>
        </Group>
    );
}

export default function JudgesTab({
    tablesCount,
    allJudgesOptions,
    assignedJudgesOptions,
    headJudgeId,    setHeadJudgeId,
    finalJudgeId,   setFinalJudgeId,
    tournamentJudges, setTournamentJudges,
    tableJudgesMap, setTableJudgesMap,
    onSaveJudges,
}) {
    const c = useThemeColors();
    const renderOption = ({ option }) => <JudgeOption option={option} />;

    return (
        <Stack gap="lg">
            <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
                <Title order={4} mb="md">Судейский состав</Title>

                <Group align="flex-end" mb="md">
                    <Select
                        label="Главный судья (ГС)"
                        description="Отвечает за весь турнир, видит скрытые результаты"
                        data={allJudgesOptions}
                        renderOption={renderOption}
                        searchable clearable
                        value={headJudgeId || null}
                        onChange={val => setHeadJudgeId(val || '')}
                        style={{ flex: 1 }}
                    />
                    <Select
                        label="Судья финала"
                        description="Назначается на финальную стадию турнира"
                        data={allJudgesOptions}
                        renderOption={renderOption}
                        searchable clearable
                        value={finalJudgeId || null}
                        onChange={val => setFinalJudgeId(val || null)}
                        style={{ flex: 1 }}
                    />
                </Group>

                <MultiSelect
                    label="Судьи турнира"
                    description="Все судьи, участвующие в турнире (могут вести столы)"
                    data={allJudgesOptions}
                    renderOption={renderOption}
                    searchable
                    value={tournamentJudges}
                    onChange={setTournamentJudges}
                    mb="md"
                />

                <Button leftSection={<IconDeviceFloppy size={16} />} color="brandRed" onClick={onSaveJudges}>
                    Сохранить судей
                </Button>
            </Paper>

            <Paper withBorder p="md" style={{ backgroundColor: c.surface2 }}>
                <Title order={4} mb="md">Судьи по столам</Title>
                <Text size="sm" c="dimmed" mb="md">
                    Назначения сохраняются при нажатии «Сохранить судей» выше.
                </Text>
                <Table
                    styles={{ thead: { backgroundColor: c.tableHeader } }}
                >
                    <Table.Thead>
                        <Table.Tr>
                            <Table.Th>Стол</Table.Th>
                            <Table.Th>Судья</Table.Th>
                        </Table.Tr>
                    </Table.Thead>
                    <Table.Tbody>
                        {Array.from({ length: tablesCount }, (_, i) => i + 1).map(tableNum => (
                            <Table.Tr key={tableNum}>
                                <Table.Td fw={600}>Стол {tableNum}</Table.Td>
                                <Table.Td>
                                    <Select
                                        data={assignedJudgesOptions}
                                        renderOption={renderOption}
                                        searchable clearable
                                        placeholder="Выберите судью"
                                        value={tableJudgesMap[String(tableNum)]
                                            ? String(tableJudgesMap[String(tableNum)])
                                            : null}
                                        onChange={val => setTableJudgesMap(prev => ({
                                            ...prev,
                                            [String(tableNum)]: val ? Number(val) : undefined,
                                        }))}
                                        size="xs"
                                    />
                                </Table.Td>
                            </Table.Tr>
                        ))}
                    </Table.Tbody>
                </Table>
            </Paper>
        </Stack>
    );
}
