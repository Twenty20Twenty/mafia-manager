// src/App.jsx
import { MantineProvider, createTheme, localStorageColorSchemeManager } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

import MainLayout from './layout/MainLayout';
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute';

// Pages
import HomePage               from './pages/HomePage';
import LoginPage              from './pages/auth/LoginPage';
import RegisterPage           from './pages/auth/RegisterPage';
import VerifyEmailPage        from './pages/auth/VerifyEmailPage';
import ForgotPasswordPage     from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage      from './pages/auth/ResetPasswordPage';
import ClaimPhantomPage       from './pages/auth/ClaimPhantomPage.jsx';
import PlayersPage            from './pages/players/PlayersPage.jsx';
import PlayerProfilePage      from './pages/players/PlayerProfilePage.jsx';
import EditPlayerPage         from './pages/players/EditPlayerPage.jsx';
import ClubsPage              from './pages/clubs/ClubsPage.jsx';
import ClubProfilePage        from './pages/clubs/ClubProfilePage.jsx';
import CreateClubPage         from './pages/clubs/CreateClubPage';
import EditClubPage           from './pages/clubs/EditClubPage';
import JudgesPage             from './pages/judges/JudgesPage.jsx';
import ManageJudgesPage       from './pages/admin/ManageJudgesPage.jsx';
import AdminDashboard         from './pages/admin/AdminDashboard';
import CreatePhantomPage      from './pages/admin/CreatePhantomPage';
import ManageClubRightsPage   from './pages/admin/ManageClubRightsPage';
import ManageUsersPage        from './pages/admin/ManageUsersPage.jsx';
import TournamentsPage        from './pages/tournaments/TournamentsPage';
import TournamentProfilePage  from './pages/tournaments/TournamentProfilePage';
import CreateTournamentPage   from './pages/tournaments/CreateTournamentPage.jsx';
import TournamentManagementPage from './pages/tournaments/TournamentManagementPage.jsx';
import GameProtocolPage       from './pages/tournaments/GameProtocolPage';

const colorSchemeManager = localStorageColorSchemeManager({
    key: 'mafia-manager-color-scheme',
});

const theme = createTheme({
    primaryColor: 'brandRed',
    colors: {
        brandRed: [
            '#FFE3E3', '#FFC9C9', '#FFA8A8', '#FF8787', '#FF6B6B',
            '#FA5252', '#F03E3E', '#E03131', '#C92A2A', '#B02525',
        ],
    },
    fontFamily: 'Verdana, sans-serif',

    // Принудительно задаём светлые цвета компонентов для light-темы
    components: {
        Paper: {
            defaultProps: {
                // Не задаём bg здесь — используем CSS-переменные в index.css
            },
        },
        Table: {
            styles: (theme) => ({
                thead: {
                    // перекрывается через styles prop или CSS vars
                },
            }),
        },
    },
});

function App() {
    return (
        <MantineProvider
            theme={theme}
            colorSchemeManager={colorSchemeManager}
            defaultColorScheme="dark"
        >
            <Notifications position="top-right" zIndex={9999} />

            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<MainLayout />}>

                        {/* ── Публичные ────────────────────────────────── */}
                        <Route index element={<HomePage />} />
                        <Route path="auth"            element={<LoginPage />} />
                        <Route path="register"        element={<RegisterPage />} />
                        <Route path="verify-email"    element={<VerifyEmailPage />} />
                        <Route path="forgot-password" element={<ForgotPasswordPage />} />
                        <Route path="reset-password"  element={<ResetPasswordPage />} />
                        <Route path="claim-phantom"   element={<ClaimPhantomPage />} />

                        {/* Публичное чтение */}
                        <Route path="clubs"            element={<ClubsPage />} />
                        <Route path="clubs/:id"        element={<ClubProfilePage />} />
                        <Route path="players"          element={<PlayersPage />} />
                        <Route path="players/:id"      element={<PlayerProfilePage />} />
                        <Route path="judges"           element={<JudgesPage />} />
                        <Route path="tournaments"      element={<TournamentsPage />} />
                        <Route path="tournaments/:id"  element={<TournamentProfilePage />} />

                        {/* ── Требуют авторизации ───────────────────────── */}
                        <Route path="create-club" element={
                            <ProtectedRoute><CreateClubPage /></ProtectedRoute>
                        } />
                        <Route path="clubs/:id/edit" element={
                            <ProtectedRoute><EditClubPage /></ProtectedRoute>
                        } />
                        <Route path="players/:id/edit" element={
                            <ProtectedRoute><EditPlayerPage /></ProtectedRoute>
                        } />
                        <Route path="create-tournament" element={
                            <ProtectedRoute><CreateTournamentPage /></ProtectedRoute>
                        } />
                        <Route path="tournaments/:id/manage" element={
                            <ProtectedRoute><TournamentManagementPage /></ProtectedRoute>
                        } />
                        <Route path="tournaments/:id/games/new" element={
                            <ProtectedRoute>
                                <GameProtocolPage isNewRatingGame={true} />
                            </ProtectedRoute>
                        } />
                        <Route path="tournaments/:id/games/:gameId" element={
                            <ProtectedRoute><GameProtocolPage /></ProtectedRoute>
                        } />

                        {/* ── Только Admin ─────────────────────────────── */}
                        <Route path="judges/manage" element={
                            <AdminRoute><ManageJudgesPage /></AdminRoute>
                        } />
                        <Route path="admin" element={
                            <AdminRoute><AdminDashboard /></AdminRoute>
                        } />
                        <Route path="admin/create-user" element={
                            <AdminRoute><CreatePhantomPage /></AdminRoute>
                        } />
                        <Route path="admin/clubs" element={
                            <AdminRoute><ManageClubRightsPage /></AdminRoute>
                        } />
                        <Route path="admin/users" element={
                            <AdminRoute><ManageUsersPage /></AdminRoute>
                        } />

                    </Route>
                </Routes>
            </BrowserRouter>
        </MantineProvider>
    );
}

export default App;
