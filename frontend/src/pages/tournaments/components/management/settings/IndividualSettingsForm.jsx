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

/** Конвертирует List<Integer> → строку "30 20 10" для отображения в поле */
export function swissTiersToString(tiers) {
    if (!tiers || !Array.isArray(tiers)) return '';
    return tiers.join(' ');
}

/**
 * Парсит строку "30 20 10" → [30, 20, 10].
 * Игнорирует нецифровые токены. Возвращает null при пустом вводе.
 */
export function parseSwissTiers(str) {
    if (!str || !str.trim()) return null;
    const nums = str
        .trim()
        .split(/[\s,;]+/)
        .map(s => parseInt(s, 10))
        .filter(n => !isNaN(n) && n > 0);
    return nums.length > 0 ? nums : null;
}

/** Проверяет, что сумма тиров кратна числу участников (кратна 10) */
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

    // Локальная строковая версия тиров для TextInput
    const tiersStr = swissTiersToString(settings.swissTiers);
    const tiersErr = settings.isSwissSystem && settings.swissTiers
        ? validateSwissTiers(settings.swissTiers, settings.maxParticipants)
        : null;

    const handleTiersChange = (raw) => {
        const parsed = parseSwissTiers(raw);
        // Сохраняем сырую строку для отображения, а в settings — распарсенный массив
        set('swissTiersRaw', raw);
        set('swissTiers', parsed);
    };

    // Показываем либо сырую строку (если редактируется), либо форматированную
    const displayTiers = settings.swissTiersRaw !== undefined
        ? settings.swissTiersRaw
        : tiersStr;

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
                            onBlur={() => {
                                // При потере фокуса — форматируем строку из распарсенного массива
                                set('swissTiersRaw', undefined);
                            }}
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
