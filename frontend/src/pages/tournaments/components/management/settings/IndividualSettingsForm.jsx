// src/pages/tournaments/components/management/settings/IndividualSettingsForm.jsx
import {
    Stack, Paper, Title, Select, Switch,
    NumberInput, Textarea, TextInput, Group, Text
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { IconCalendar } from '@tabler/icons-react';

const STATUS_OPTIONS = [
    { value: 'registration', label: 'Регистрация' },
    { value: 'active',       label: 'Идёт'        },
    { value: 'completed',    label: 'Завершён'     },
    { value: 'archived',     label: 'Архив'        },
];

const FINAL_COEFFICIENT_OPTIONS = [
    { value: '1',   label: '1.0 (без коэффициента)' },
    { value: '1.1', label: '1.1'                     },
    { value: '1.2', label: '1.2'                     },
    { value: '1.3', label: '1.3'                     },
    { value: '1.4', label: '1.4'                     },
    { value: '1.5', label: '1.5'                     },
];

export function swissTiersToString(tiers) {
    if (!tiers || !Array.isArray(tiers)) return '';
    return tiers.join(' ');
}

export function parseSwissTiers(str) {
    if (!str || !str.trim()) return null;
    const nums = str
        .trim()
        .split(/[\s,;]+/)
        .map(s => parseInt(s, 10))
        .filter(n => !isNaN(n) && n > 0);
    return nums.length > 0 ? nums : null;
}

export function validateSwissTiers(tiers, maxParticipants) {
    if (!tiers) return null;
    const sum = tiers.reduce((a, b) => a + b, 0);
    if (sum !== maxParticipants) {
        return `Сумма тиров (${sum}) должна равняться кол-ву участников (${maxParticipants})`;
    }
    const notMultiple = tiers.find(t => t % 10 !== 0);
    if (notMultiple !== undefined) {
        return `Каждый тир должен быть кратен 10 (найдено: ${notMultiple})`;
    }
    return null;
}

export default function IndividualSettingsForm({ settings, setSettings, citiesData }) {
    const set = (field, val) => setSettings(prev => ({ ...prev, [field]: val }));

    const tiersStr = swissTiersToString(settings.swissTiers);
    const tiersErr = settings.isSwissSystem && settings.swissTiers
        ? validateSwissTiers(settings.swissTiers, settings.maxParticipants)
        : null;

    const handleTiersChange = (raw) => {
        const parsed = parseSwissTiers(raw);
        set('swissTiersRaw', raw);
        set('swissTiers', parsed);
    };

    const displayTiers = settings.swissTiersRaw !== undefined
        ? settings.swissTiersRaw
        : tiersStr;

    const handleSingleDayToggle = (checked) => {
        set('singleDay', checked);
        // При переключении сбрасываем даты
        if (checked) {
            // single mode: берём первую дату как единственную
            const first = settings.dates?.[0] ?? null;
            set('dates', [first, first]);
        } else {
            // range mode: оставляем текущие
        }
    };

    const handleSingleDateChange = (val) => {
        set('dates', [val, val]);
    };

    const isSingleDay = settings.singleDay ?? false;

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

                {/* Переключатель один день / диапазон */}
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

            {/* Параметры игры */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Параметры турнира</Title>

                <Group grow mb="md">
                    <NumberInput
                        label="Макс. участников"
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

                <Group grow mb="md">
                    <NumberInput
                        label="Кол-во финальных туров"
                        value={settings.finalRoundsCount}
                        onChange={val => set('finalRoundsCount', val)}
                        min={0}
                    />
                    <Select
                        label="Коэффициент финала"
                        data={FINAL_COEFFICIENT_OPTIONS}
                        value={String(settings.finalCoefficient ?? 1)}
                        onChange={val => set('finalCoefficient', parseFloat(val))}
                    />
                </Group>
            </Paper>

            {/* Швейцарская система */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Швейцарская рассадка</Title>

                <Switch
                    label="Использовать швейцарскую систему рассадки"
                    description="Участники распределяются по результатам предыдущих туров"
                    checked={settings.isSwissSystem ?? false}
                    onChange={e => set('isSwissSystem', e.currentTarget.checked)}
                    mb="md"
                />

                {settings.isSwissSystem && (
                    <>
                        <NumberInput
                            label="С какого тура начинается швейцарская рассадка"
                            description="Например: 8 — туры 1–7 обычные, 8+ по швейцарке"
                            value={settings.swissRoundsStart ?? 1}
                            onChange={val => set('swissRoundsStart', val)}
                            min={1}
                            max={settings.roundsCount || 10}
                            mb="md"
                        />

                        <TextInput
                            label="Тиры (распределение участников по группам)"
                            description={
                                `Введите размеры групп через пробел: "30 20 10". ` +
                                `Сумма должна = ${settings.maxParticipants} участников, ` +
                                `каждый тир кратен 10.`
                            }
                            placeholder="Например: 30 20 10"
                            value={displayTiers}
                            onChange={e => handleTiersChange(e.currentTarget.value)}
                            error={tiersErr}
                            onBlur={() => set('swissTiersRaw', undefined)}
                        />

                        {settings.swissTiers && !tiersErr && (
                            <Text size="xs" c="dimmed" mt={4}>
                                Тиров: {settings.swissTiers.length} —
                                {settings.swissTiers.map((t, i) => ` Группа ${i + 1}: ${t} уч.`).join(',')}
                            </Text>
                        )}
                    </>
                )}
            </Paper>

            {/* Видимость и фиксация */}
            <Paper withBorder p="md">
                <Title order={4} mb="md">Дополнительно</Title>

                <Switch
                    label="Скрыть результаты от участников"
                    description="Очки видны только организатору и главному судье"
                    checked={settings.areResultsHidden}
                    onChange={e => set('areResultsHidden', e.currentTarget.checked)}
                    mb="md"
                />

                <Switch
                    label="Зафиксировать отборочных"
                    description="Список финалистов зафиксирован, изменения невозможны"
                    checked={settings.areQualifiersFixed}
                    onChange={e => set('areQualifiersFixed', e.currentTarget.checked)}
                />
            </Paper>
        </Stack>
    );
}
