// src/components/AvatarUpload.jsx
import { useRef } from 'react';
import { Avatar, ActionIcon, Tooltip, Text, Stack, Box } from '@mantine/core';
import { IconCamera, IconX } from '@tabler/icons-react';

/**
 * Переиспользуемый компонент загрузки аватара.
 * Клик по аватару открывает file picker, показывает предпросмотр.
 *
 * Props:
 *   displayUrl   - текущий URL (предпросмотр или сохранённый)
 *   nickname     - для fallback-буквы в Avatar
 *   onFileSelect - callback(File) при выборе файла
 *   onReset      - callback() при сбросе (если есть новый файл)
 *   hasNewFile   - есть ли незагруженный файл (показывает кнопку сброса)
 *   error        - строка ошибки валидации
 *   size         - размер аватара (default: 100)
 *   loading      - показывает overlay загрузки
 */
export default function AvatarUpload({
    displayUrl,
    nickname = '',
    onFileSelect,
    onReset,
    hasNewFile = false,
    error = null,
    size = 100,
    loading = false,
}) {
    const inputRef = useRef(null);

    const handleClick = () => {
        if (!loading) inputRef.current?.click();
    };

    const handleChange = (e) => {
        const file = e.target.files?.[0] ?? null;
        onFileSelect(file);
        // Сбрасываем input чтобы можно было выбрать тот же файл повторно
        e.target.value = '';
    };

    return (
        <Stack align="center" gap="xs">
            <Box style={{ position: 'relative', display: 'inline-block' }}>
                {/* Аватар с hover-эффектом */}
                <Avatar
                    src={displayUrl}
                    size={size}
                    radius={size}
                    color="brandRed"
                    style={{
                        cursor: loading ? 'not-allowed' : 'pointer',
                        opacity: loading ? 0.6 : 1,
                        transition: 'opacity 0.2s',
                    }}
                    onClick={handleClick}
                >
                    {nickname?.[0]?.toUpperCase()}
                </Avatar>

                {/* Иконка камеры поверх аватара */}
                <Tooltip label="Загрузить фото" withArrow position="bottom">
                    <ActionIcon
                        variant="filled"
                        color="brandRed"
                        size="sm"
                        radius="xl"
                        style={{
                            position: 'absolute',
                            bottom: 0,
                            right: 0,
                            pointerEvents: loading ? 'none' : 'auto',
                        }}
                        onClick={handleClick}
                    >
                        <IconCamera size={12} />
                    </ActionIcon>
                </Tooltip>

                {/* Кнопка сброса (только если выбран новый файл) */}
                {hasNewFile && !loading && (
                    <Tooltip label="Отменить" withArrow position="bottom">
                        <ActionIcon
                            variant="filled"
                            color="gray"
                            size="sm"
                            radius="xl"
                            style={{
                                position: 'absolute',
                                bottom: 0,
                                left: 0,
                            }}
                            onClick={onReset}
                        >
                            <IconX size={12} />
                        </ActionIcon>
                    </Tooltip>
                )}
            </Box>

            {/* Подсказка */}
            {!hasNewFile && (
                <Text size="xs" c="dimmed">Нажмите для загрузки</Text>
            )}
            {hasNewFile && (
                <Text size="xs" c="teal">Новое фото выбрано</Text>
            )}

            {/* Ошибка валидации */}
            {error && (
                <Text size="xs" c="red">{error}</Text>
            )}

            {/* Скрытый input */}
            <input
                ref={inputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                style={{ display: 'none' }}
                onChange={handleChange}
            />
        </Stack>
    );
}
