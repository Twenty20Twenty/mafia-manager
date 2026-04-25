// src/pages/tournaments/components/management/SettingsTab.jsx
// Диспетчер настроек по типу турнира.
// Рендерит нужную форму (Season / Individual / Team) + кнопку сохранения.

import { Stack, Button, Select, Paper, Title, Text, Avatar, Group } from '@mantine/core';
import { IconDeviceFloppy } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import api from '../../../../api/axios';
import SeasonSettingsForm     from './settings/SeasonSettingsForm';
import IndividualSettingsForm from './settings/IndividualSettingsForm';
import TeamSettingsForm       from './settings/TeamSettingsForm';

function JudgeOption({ option }) {
    return (
        <Group gap="sm" w="100%">
            <Avatar src={option.avatar} size="xs" radius="xl" />
            <Text size="sm">{option.label}</Text>
        </Group>
    );
}

export default function SettingsTab({
    settings, setSettings,
    isRating, isTeam,
    allJudgesOptions,
    headJudgeId, setHeadJudgeId,
    onSave,
}) {
    const [citiesData, setCitiesData] = useState([]);

    useEffect(() => {
        api.get('/geo/cities')
            .then(res => setCitiesData(res.data.map(c => ({ value: String(c.id), label: c.name }))))
            .catch(console.error);
    }, []);

    const renderOption = ({ option }) => <JudgeOption option={option} />;

    const SettingsForm = isRating ? SeasonSettingsForm
        : isTeam          ? TeamSettingsForm
        :                   IndividualSettingsForm;

    return (
        <Stack gap="lg">
            {/* Главный судья — для всех типов */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Главный судья</Title>
                <Select
                    label="Главный судья турнира"
                    description="Имеет доступ ко всем настройкам и протоколам"
                    data={allJudgesOptions}
                    renderOption={renderOption}
                    searchable clearable
                    value={headJudgeId || null}
                    onChange={val => setHeadJudgeId(val || '')}
                />
            </Paper>

            {/* Форма настроек по типу турнира */}
            <SettingsForm
                settings={settings}
                setSettings={setSettings}
                citiesData={citiesData}
            />

            <Button
                leftSection={<IconDeviceFloppy size={16} />}
                color="brandRed"
                onClick={onSave}
            >
                Сохранить настройки
            </Button>
        </Stack>
    );
}
