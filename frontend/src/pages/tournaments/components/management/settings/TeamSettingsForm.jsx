// src/pages/tournaments/components/management/settings/TeamSettingsForm.jsx
// Настройки для командного турнира (team)
import { Stack, Paper, Title, Select, Switch, NumberInput, Textarea, TextInput, Group } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconCalendar } from '@tabler/icons-react';

const STATUS_OPTIONS = [
    { value: 'registration', label: 'Регистрация' },
    { value: 'active',       label: 'Идёт'        },
    { value: 'completed',    label: 'Завершён'     },
    { value: 'archived',     label: 'Архив'        },
];

export default function TeamSettingsForm({ settings, setSettings, citiesData }) {
    const set = (field, val) => setSettings(prev => ({ ...prev, [field]: val }));

    return (
        <Stack gap="lg">
            {/* Основная информация */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Основные настройки</Title>

                <TextInput
                    label="Название"
                    value={settings.title || ''}
                    onChange={e => set('title', e.currentTarget.value)}
                    mb="md"
                />

                <Textarea
                    label="Описание"
                    value={settings.description}
                    onChange={e => set('description', e.currentTarget.value)}
                    autosize minRows={2}
                    mb="md"
                />

                <Select
                    label="Город проведения"
                    data={citiesData}
                    searchable clearable
                    placeholder="Выберите город"
                    value={settings.cityId ? String(settings.cityId) : null}
                    onChange={val => set('cityId', val ? Number(val) : null)}
                    mb="md"
                />

                <Select
                    label="Статус турнира"
                    data={STATUS_OPTIONS}
                    value={settings.status}
                    onChange={val => set('status', val)}
                    mb="md"
                />

                <DatePickerInput
                    type="range"
                    label="Даты проведения"
                    leftSection={<IconCalendar size={16} />}
                    value={settings.dates}
                    onChange={val => set('dates', val)}
                    mb="md"
                />

                <TextInput
                    label="Ссылка на соц. сеть"
                    placeholder="https://vk.com/..."
                    value={settings.link}
                    onChange={e => set('link', e.currentTarget.value)}
                    mb="md"
                />
            </Paper>

            {/* Параметры командного турнира */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Параметры турнира</Title>

                <Group grow mb="md">
                    <NumberInput
                        label="Макс. участников"
                        description="Максимальное количество игроков"
                        value={settings.maxParticipants}
                        onChange={val => set('maxParticipants', val)}
                        min={10} step={10}
                    />
                    <NumberInput
                        label="Кол-во отборочных туров"
                        value={settings.roundsCount}
                        onChange={val => set('roundsCount', val)}
                        min={1}
                    />
                </Group>

                <NumberInput
                    label="Количество игроков в команде"
                    value={settings.teamSize ?? 5}
                    onChange={val => set('teamSize', val)}
                    min={1} max={20}
                    mb="md"
                    style={{ maxWidth: 220 }}
                />
            </Paper>

            {/* Видимость */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Дополнительно</Title>

                <Switch
                    label="Скрыть результаты от участников"
                    description="Очки видны только организатору и главному судье"
                    checked={settings.areResultsHidden}
                    onChange={e => set('areResultsHidden', e.currentTarget.checked)}
                />
            </Paper>
        </Stack>
    );
}
