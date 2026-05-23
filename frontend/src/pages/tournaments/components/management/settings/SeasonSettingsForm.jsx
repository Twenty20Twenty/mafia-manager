// src/pages/tournaments/components/management/settings/SeasonSettingsForm.jsx
import { Stack, Paper, Title, Select, Switch, NumberInput, Textarea, TextInput, Group, Text } from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconCalendar } from '@tabler/icons-react';

const STATUS_OPTIONS = [
    { value: 'registration', label: 'Регистрация' },
    { value: 'active',       label: 'Идёт'        },
    { value: 'completed',    label: 'Завершён'     },
    { value: 'archived',     label: 'Архив'        },
];

export default function SeasonSettingsForm({ settings, setSettings, citiesData }) {
    const set = (field, val) => setSettings(prev => ({ ...prev, [field]: val }));

    const isSingleDay = settings.singleDay ?? false;

    const handleSingleDayToggle = (checked) => {
        set('singleDay', checked);
        if (checked) {
            const first = settings.dates?.[0] ?? null;
            set('dates', [first, first]);
        }
    };

    const handleSingleDateChange = (val) => {
        set('dates', [val, val]);
    };

    return (
        <Stack gap="lg">
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

                <Group justify="space-between" mb="xs">
                    <Text size="sm" fw={500}>Даты проведения</Text>
                    <Switch
                        label="Один день"
                        size="sm"
                        checked={isSingleDay}
                        onChange={e => handleSingleDayToggle(e.currentTarget.checked)}
                    />
                </Group>

                {isSingleDay ? (
                    <DatePickerInput
                        placeholder="Выберите дату"
                        leftSection={<IconCalendar size={16} />}
                        value={settings.dates?.[0] ?? null}
                        onChange={handleSingleDateChange}
                        mb="md"
                    />
                ) : (
                    <DatePickerInput
                        type="range"
                        placeholder="Выберите период"
                        leftSection={<IconCalendar size={16} />}
                        value={settings.dates}
                        onChange={val => set('dates', val)}
                        mb="md"
                    />
                )}

                <TextInput
                    label="Ссылка на соц. сеть"
                    placeholder="https://vk.com/..."
                    value={settings.link}
                    onChange={e => set('link', e.currentTarget.value)}
                    mb="md"
                />
            </Paper>

            <Paper withBorder p="md">
                <Title order={4} mb="md">Параметры рейтинга</Title>
                <NumberInput
                    label="Порог рейтинга (%)"
                    description="Минимальный % игр для включения в рейтинг (0–100)"
                    value={settings.ratingThreshold ?? 0}
                    onChange={val => set('ratingThreshold', val)}
                    min={0} max={100} step={5}
                    mb="md"
                />
            </Paper>

            <Paper withBorder p="md">
                <Title order={4} mb="md">Видимость результатов</Title>
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
