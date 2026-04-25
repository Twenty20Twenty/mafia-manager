// src/pages/clubs/EditClubPage.jsx
import { Container, Title, TextInput, Textarea, Button, Paper, Group, Center, Loader } from '@mantine/core';
import { IconArrowLeft, IconDeviceFloppy } from '@tabler/icons-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import api from '../../api/axios';
import { uploadClubAvatar } from '../../api/avatar';
import { useAvatarUpload } from '../../hooks/useAvatarUpload';
import AvatarUpload from '../../components/AvatarUpload';

export default function EditClubPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const [formData, setFormData] = useState({
        name: '', city: '', description: '', socialLink: '', logoUrl: ''
    });

    // Хук управления логотипом клуба
    const avatar = useAvatarUpload(formData.logoUrl || null);

    useEffect(() => {
        api.get(`/clubs/${id}`).then(res => {
            console.log(res);
            setFormData({
                name:        res.data.name        || '',
                city:        res.data.city        || '',
                description: res.data.description || '',
                socialLink:  res.data.socialLink  || '',
                logoUrl:     res.data.logoUrl     || '',
            });
            setLoading(false);
        }).catch(() => {
            alert('Ошибка загрузки клуба');
            navigate('/clubs');
        });
    }, [id, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            // 1. Загружаем новый логотип если выбран
            if (avatar.hasNewFile) {
                await uploadClubAvatar(Number(id), avatar.file);
            }
            console.log(formData);
            // 2. Обновляем остальные данные клуба
            await api.put(`/clubs/${id}`, formData);

            alert('Данные клуба обновлены!');
            navigate(`/clubs/${id}`);
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка сохранения');
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;

    return (
        <Container size="sm" py="xl">
            <Button component={Link} to={`/clubs/${id}`} variant="subtle" color="gray" leftSection={<IconArrowLeft size={16}/>} mb="md">
                Вернуться в профиль
            </Button>
            <Title order={2} mb="lg">Редактирование клуба</Title>

            <Paper withBorder p="xl" radius="md">
                <form onSubmit={handleSubmit}>
                    {/* Логотип клуба */}
                    <Group justify="center" mb="xl">
                        <AvatarUpload
                            displayUrl={avatar.displayUrl}
                            nickname={formData.name}
                            onFileSelect={avatar.handleFileSelect}
                            onReset={avatar.reset}
                            hasNewFile={avatar.hasNewFile}
                            error={avatar.error}
                            loading={saving}
                            size={100}
                        />
                    </Group>

                    <TextInput
                        label="Название клуба" required
                        value={formData.name}
                        onChange={e => setFormData({...formData, name: e.currentTarget.value})}
                    />
                    <TextInput
                        label="Город" required mt="md"
                        value={formData.city}
                        onChange={e => setFormData({...formData, city: e.currentTarget.value})}
                    />
                    <Textarea
                        label="Описание" mt="md" minRows={3}
                        value={formData.description}
                        onChange={e => setFormData({...formData, description: e.currentTarget.value})}
                    />
                    <TextInput
                        label="Ссылка на соц. сеть" mt="md"
                        value={formData.socialLink}
                        onChange={e => setFormData({...formData, socialLink: e.currentTarget.value})}
                    />

                    <Group justify="flex-end" mt="xl">
                        <Button variant="default" onClick={() => navigate(`/clubs/${id}`)}>Отмена</Button>
                        <Button type="submit" loading={saving} leftSection={<IconDeviceFloppy size={16}/>} color="brandRed">
                            Сохранить
                        </Button>
                    </Group>
                </form>
            </Paper>
        </Container>
    );
}
