package com.mafia.manager.entity;

/**
 * Типы операций в аудит-логе игровых протоколов.
 */
public enum GameAuditAction {
    /** Создание новой пустой игры */
    GAME_CREATED,

    /** Сохранение протокола в статусе draft */
    PROTOCOL_DRAFT_SAVED,

    /** Завершение игры (status → completed) */
    GAME_COMPLETED,

    /** Удаление одной игры */
    GAME_DELETED,

    /** Массовое удаление игр (ALL / ROUND / RANGE) */
    GAMES_BULK_DELETED,

    /** Замена игрока в слоте незавершённой игры */
    SLOT_PLAYER_SWAPPED,

    /**
     * Изменение числовых баллов в слоте:
     * extra_points_positive, extra_points_negative, penalty_points.
     * Логируется только при реальном изменении (старое ≠ новое).
     */
    SLOT_SCORES_CHANGED,
}
