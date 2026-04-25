import { useState, useEffect } from 'react';
import { Container, Title, TextInput, Textarea, Button, Paper, Group, Select } from '@mantine/core';
import { IconArrowLeft } from '@tabler/icons-react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../api/axios';

export default function CreateClubPage() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [cities, setCities] = useState([]);
    const [formData, setFormData] = useState({ name: '', city: '', description: '', socialLink: '' });

    useEffect(() => {
        api.get('/geo/cities').then(res => setCities(res.data.map(c => ({ value: c.name, label: c.name }))));
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const res = await api.post('/clubs', formData);
            alert('Клуб успешно создан!');
            navigate(`/clubs/${res.data.id}`);
        } catch (error) {
            alert(error.response?.data?.message || 'Ошибка создания клуба');
        } finally {
            setLoading(false);
        }
    };

    return (
        <Container size="sm" py="xl">
            <Button component={Link} to="/clubs" variant="subtle" color="gray" leftSection={<IconArrowLeft size={16}/>} mb="md">
                Назад к списку
            </Button>
            <Title order={2} mb="lg">Создание нового клуба</Title>
            <Paper withBorder p="xl" radius="md">
                <form onSubmit={handleSubmit}>
                    <TextInput label="Название клуба" required value={formData.name} onChange={e => setFormData({...formData, name: e.currentTarget.value})} />
                    <Select label="Город" data={cities} searchable mt="md" required value={formData.city} onChange={v => setFormData({...formData, city: v})} />
                    <Textarea label="Описание" mt="md" minRows={3} value={formData.description} onChange={e => setFormData({...formData, description: e.currentTarget.value})} />
                    <TextInput label="Ссылка на соц. сеть" mt="md" value={formData.socialLink} onChange={e => setFormData({...formData, socialLink: e.currentTarget.value})} />
                    <Group justify="flex-end" mt="xl">
                        <Button variant="default" onClick={() => navigate('/clubs')}>Отмена</Button>
                        <Button type="submit" color="brandRed" loading={loading}>Создать клуб</Button>
                    </Group>
                </form>
            </Paper>
        </Container>
    );
}
