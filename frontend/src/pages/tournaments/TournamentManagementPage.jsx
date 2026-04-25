// src/pages/tournaments/TournamentManagementPage.jsx
// Изменение: передаём tournamentId в SeedingTab для статуса рассадки (БАГ 4)
import {Container, Title, Button, Tabs, Alert, Center, Loader, Text, Box} from '@mantine/core';
import { IconArrowLeft, IconUsers, IconGavel, IconTable, IconSettings } from '@tabler/icons-react';
import { useParams, Link } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTournamentManagement } from './hooks/useTournamentManagement';
import { resolveCanManage } from './utils/tournamentUtils';
import ParticipantsTab from './components/management/ParticipantsTab';
import JudgesTab       from './components/management/JudgesTab';
import SeedingTab      from './components/management/SeedingTab';
import SettingsTab     from './components/management/SettingsTab';

export default function TournamentManagementPage() {
    const { id } = useParams();
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState('participants');

    const {
        tournament, loading, settings, setSettings,
        approvedParticipants, pendingRequests,
        selectedPlayerId, setSelectedPlayerId,
        headJudgeId, setHeadJudgeId,
        finalJudgeId, setFinalJudgeId,
        tournamentJudges, setTournamentJudges,
        tableJudgesMap, setTableJudgesMap,
        exceptions, exceptionP1, setExceptionP1, exceptionP2, setExceptionP2,
        addPlayerOptions, allJudgesOptions, assignedJudgesOptions, participantsForExceptions,
        finalists, finalistsLoading, finalistsLocked,
        handleSaveSettings,
        handleSaveJudges,
        handleAcceptRequest, handleRejectRequest,
        handleAddManual, handleRemoveParticipant,
        handleAddException, handleRemoveException,
        handleGenerateSeeding,
        handleFixQualifiers,
        handleGenerateFinalSeeding,
        handleDeleteGames,
        handleAutoFillFinalists,
        handleLockFinalists,
        handleUnlockFinalists,
        handleSaveFinalists
    } = useTournamentManagement(id);

    if (loading) return <Center py="xl"><Loader color="brandRed" /></Center>;

    const canManage = resolveCanManage(user, tournament);
    if (!tournament || !canManage) {
        return (
            <Container py="xl">
                <Alert color="red">Доступ запрещен или турнир не найден</Alert>
            </Container>
        );
    }

    const isRating    = tournament.type === 'season';
    const isTeam      = tournament.type === 'team';
    const tablesCount = Math.ceil(approvedParticipants.length / 10) || 1;

    return (
        <Container size="lg" py="xl">
            <Button
                component={Link} to={`/tournaments/${id}`}
                variant="subtle" color="gray"
                leftSection={<IconArrowLeft size={16} />}
                mb="md"
            >
                Назад к турниру
            </Button>

            <Title order={2} mb="xl" lineClamp={2}>
                Управление: <Text span c="dimmed" fw={400}>{tournament.title}</Text>
            </Title>

            <Tabs value={activeTab} onChange={setActiveTab} variant="outline" radius="md">
                <Tabs.List mb="md" grow justify="flex-start">
                    <Tabs.Tab value="participants" leftSection={<IconUsers size={16} />}>
                        <Box style={{ textAlign: 'left' }}>
                            <Text visibleFrom="xs">Участники</Text>
                            <Text hiddenFrom="xs">Участ.</Text>
                        </Box>
                    </Tabs.Tab>
                    {!isRating && (
                        <Tabs.Tab value="judges" leftSection={<IconGavel size={16} />}>
                            <Box style={{ textAlign: 'left' }}>
                                <Text visibleFrom="xs">Судьи</Text>
                            </Box>
                        </Tabs.Tab>
                    )}
                    {!isRating && (
                        <Tabs.Tab value="seeding" leftSection={<IconTable size={16} />}>
                            <Box style={{ textAlign: 'left' }}>
                                <Text visibleFrom="xs">Рассадка</Text>
                            </Box>
                        </Tabs.Tab>
                    )}
                    <Tabs.Tab value="settings" leftSection={<IconSettings size={16} />}>
                        <Box style={{ textAlign: 'left' }}>
                            <Text visibleFrom="xs">Настройки</Text>
                        </Box>
                    </Tabs.Tab>
                </Tabs.List>

                <Tabs.Panel value="participants" pt="md">
                    <ParticipantsTab
                        isRating={isRating}
                        isTeam={isTeam}
                        approvedParticipants={approvedParticipants}
                        pendingRequests={pendingRequests}
                        addPlayerOptions={addPlayerOptions}
                        selectedPlayerId={selectedPlayerId}
                        setSelectedPlayerId={setSelectedPlayerId}
                        onAddManual={handleAddManual}
                        onAccept={handleAcceptRequest}
                        onReject={handleRejectRequest}
                        onRemove={handleRemoveParticipant}
                    />
                </Tabs.Panel>

                {!isRating && (
                    <Tabs.Panel value="judges" pt="md">
                        <JudgesTab
                            tablesCount={tablesCount}
                            allJudgesOptions={allJudgesOptions}
                            assignedJudgesOptions={assignedJudgesOptions}
                            headJudgeId={headJudgeId}        setHeadJudgeId={setHeadJudgeId}
                            finalJudgeId={finalJudgeId}      setFinalJudgeId={setFinalJudgeId}
                            tournamentJudges={tournamentJudges} setTournamentJudges={setTournamentJudges}
                            tableJudgesMap={tableJudgesMap}  setTableJudgesMap={setTableJudgesMap}
                            onSaveJudges={handleSaveJudges}
                        />
                    </Tabs.Panel>
                )}

                {!isRating && (
                    <Tabs.Panel value="seeding" pt="md">
                        <SeedingTab
                            // БАГ 4 FIX: передаём tournamentId
                            tournamentId={Number(id)}
                            exceptions={exceptions}
                            exceptionP1={exceptionP1}
                            exceptionP2={exceptionP2}
                            setExceptionP1={setExceptionP1}
                            setExceptionP2={setExceptionP2}
                            peopleForExceptions={participantsForExceptions}
                            isSeedingGenerated={settings.isSeedingGenerated}
                            onAddException={handleAddException}
                            onRemoveException={handleRemoveException}
                            onGenerateSeeding={handleGenerateSeeding}
                            onFixQualifiers={handleFixQualifiers}
                            onDeleteGames={handleDeleteGames}
                            finalists={finalists}
                            allParticipants={approvedParticipants.map(p => ({ value: String(p.id), label: p.nickname }))}
                            locked={finalistsLocked}
                            onSaveFinalists={handleSaveFinalists}
                            onAutoFillFinalists={handleAutoFillFinalists}
                            onLockFinalists={handleLockFinalists}
                            onUnlockFinalists={handleUnlockFinalists}
                            finalistsLoading={finalistsLoading}
                            areQualifiersFixed={settings.areQualifiersFixed}
                            onGenerateFinalSeeding={handleGenerateFinalSeeding}
                        />
                    </Tabs.Panel>
                )}

                <Tabs.Panel value="settings" pt="md">
                    <SettingsTab
                        settings={settings}
                        setSettings={setSettings}
                        isRating={isRating}
                        isTeam={isTeam}
                        allJudgesOptions={allJudgesOptions}
                        headJudgeId={headJudgeId}
                        setHeadJudgeId={setHeadJudgeId}
                        onSave={handleSaveSettings}
                    />
                </Tabs.Panel>
            </Tabs>
        </Container>
    );
}
