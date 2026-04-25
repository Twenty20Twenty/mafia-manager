// src/pages/tournaments/constants/tournamentConstants.js

export const TOURNAMENT_TYPES = {
    individual: { label: 'Личный зачет',     color: 'blue'   },
    team:       { label: 'Командный зачет',   color: 'violet' },
    season:     { label: 'Рейтинговый сезон', color: 'teal'   },
};

export const TOURNAMENT_STATUSES = {
    registration: { label: 'Регистрация открыта', color: 'green'  },
    active:       { label: 'Турнир идет',         color: 'yellow' },
    completed:    { label: 'Завершен',             color: 'gray'   },
    archived:     { label: 'Архив',                color: 'dark'   },
};

export const ROLES_DATA = [
    { value: 'civilian', label: 'Мирный', color: 'red'   },
    { value: 'sheriff',  label: 'Шериф',  color: 'yellow'},
    { value: 'mafia',    label: 'Мафия',  color: 'blue'  },
    { value: 'don',      label: 'Дон',    color: 'grape' },
];

export const ROLE_DISTRIBUTION = { sheriff: 1, don: 1, mafia: 2, civilian: 6 };

export const DEBOUNCE_MS = 400;
