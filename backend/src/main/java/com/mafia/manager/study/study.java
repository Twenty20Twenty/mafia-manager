package com.mafia.manager.study;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

public class study {
    @Data
    @AllArgsConstructor
    public static class Player {
        private Long id;
        private String nickname;
        private String role;      // "ADMIN", "JUDGE", "PLAYER"
        private Integer rating;   // может быть null у новых игроков
        private boolean active;

    }

    public static List<String> getActivePlayers(List<Player> players) {
        return players.stream()
                .filter(player -> player.isActive() && "PLAYER".equals(player.getRole()))
                .map(Player::getNickname)
                .sorted()
                .collect(Collectors.toList());
    }

    public static long countPlayersRatingNull(List<Player> players) {
        return players.stream()
                .filter(player -> player.getRating() == null)
                .count();
    }

    public static String activePlayerMaxRating(List<Player> players) {
        return players.stream()
                .filter(Player::isActive)
                .filter(player -> "PLAYER".equals(player.getRole()))
                .filter(player -> player.getRating() != null)
                .max(Comparator.comparing(Player::getRating))
                .map(Player::getNickname)
                .orElse("Нет игроков");

    }

    /**
     * Уровень 2 — Middle
     * Используй тот же список players.
     * 2.1 — Сгруппируй игроков по роли. Результат: Map<String, List<Player>>. Выведи для каждой роли количество игроков.
     * 2.2 — Построй Map<Long, String> где ключ — id игрока, значение — его nickname.
     * Это нужно для быстрого поиска по id без перебора списка.
     * 2.3 — Напиши метод:
     * java
     * public static Player getPlayerById(List<Player> players, Long id) {
     *     // найти игрока по id
     *     // если не найден — бросить IllegalArgumentException
     *     // с сообщением "Player with id {id} not found"
     * }
     * Используй Stream + Optional + исключение.
     */

    public static Map<String, Long> countPlayerByRole(List<Player> players) {
        return players.stream()
                .collect(Collectors.groupingBy(Player::getRole, Collectors.counting()));
    }

    public static Map<Long, String> getIdByNickname(List<Player> players) {
        return players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getNickname));
    }

    public static Player getPlayerById(Long id, List<Player> players) {
        return players.stream()
                .filter(player -> player.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player with id " + id + " not found"));
    }

    /**
     * Уровень 3 — Hard
     * 3.1 — Напиши метод который считает средний рейтинг активных игроков с ролью "PLAYER".
     * Игроки с rating == null — не учитываются. Если подходящих игроков нет — вернуть 0.0.
     * java
     * public static double averageRating(List<Player> players) {
     *     // ...
     * }
     * 3.2 — Напиши метод который возвращает топ-N игроков по рейтингу. Игроки с rating == null идут в конец списка. Среди игроков с одинаковым рейтингом — сортировка по никнейму.
     * java
     * public static List<Player> topN(List<Player> players, int n) {
     *     // ...
     * }
     * 3.3 — Напиши метод buildStatsReport:
     * java
     * public static Map<String, Object> buildStatsReport(List<Player> players) {
     *     // Должен вернуть Map с ключами:
     *     // "totalPlayers"   → Integer : общее количество
     *     // "activePlayers"  → Integer : количество активных
     *     // "averageRating"  → Double  : средний рейтинг активных (null не считать)
     *     // "topPlayer"      → String  : никнейм игрока с макс. рейтингом, или "—"
     *     // "byRole"         → Map<String, Long> : количество игроков по каждой роли
     * }
     */

    public static double getAverageRating(List<Player> players) {
        return players.stream()
                .filter(Player::isActive)
                .filter(player -> "PLAYER".equals(player.getRole()))
                .map(Player::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average().orElse(0.0);
    }

    /**
     * Напиши метод который возвращает топ-N игроков по рейтингу.
     * Игроки с rating == null идут в конец списка.
     * Среди игроков с одинаковым рейтингом — сортировка по никнейму.
     * @param players
     * @return
     */
    public static List<Player> topN(List<Player> players, int n) {
        return players.stream()
                .sorted(Comparator.comparing(Player::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Player::getNickname))
                .limit(n)
                .toList();
    }

    public static Map<String, Object> buildStatsReport(List<Player> players) {
        Integer totalPlayers = players.size();
        Integer activePlayers = (int) players.stream()
                .filter(Player::isActive)
                .count();
        Double averageRating = getAverageRating(players);
        String topPlayer = players.stream()
                .filter(player -> player.getRating() != null)
                .max(Comparator.comparing(Player::getRating))
                .map(Player::getNickname)
                .orElse("--");
        Map<String, Long> byRole = countPlayerByRole(players);

        Map<String, Object> result = new HashMap<>();
        result.put("totalPlayers", totalPlayers);
        result.put("activePlayers", activePlayers);
        result.put("averageRating", averageRating);
        result.put("topPlayer", topPlayer);
        result.put("byRole", byRole);

        return result;
    }

    public static void main(String[] args) {
        String out1 = """    
         * 1.1 — Получи список никнеймов всех активных игроков с ролью "PLAYER", отсортированных по алфавиту.
         * 1.2 — Посчитай сколько игроков имеют rating == null.
         * 1.3 — Найди первого активного игрока с максимальным рейтингом.
         Верни его никнейм через Optional — если таких нет, верни "Нет игроков".
         """;
        List<Player> players = List.of(
                new Player(1L, "alice", "PLAYER", 42, true),
                new Player(2L, "bob", "JUDGE", null, true),
                new Player(3L, "carl", "PLAYER", 17, false),
                new Player(4L, "diana", "PLAYER", 55, true),
                new Player(5L, "eva", "ADMIN", 88, true),
                new Player(6L, "frank", "PLAYER", null, true),
                new Player(7L, "grace", "PLAYER", 31, true),
                new Player(8L, "abe", "PLAYER", 88, true),
                new Player(9L, "yoe", "PLAYER", 88, true)
        );
        System.out.println(out1);
        System.out.println("1.1");
        System.out.println(getActivePlayers(players));
        System.out.println("1.2");
        System.out.println(countPlayersRatingNull(players));
        System.out.println("1.3");
        System.out.println(activePlayerMaxRating(players));
        System.out.println("2.1");
        Map<String, Long> playerByRole = countPlayerByRole(players);
        for (Map.Entry<String, Long> entry : playerByRole.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.println();
        System.out.println("2.2");
        System.out.println(getIdByNickname(players));
        System.out.println("2.3");
        System.out.println(getPlayerById(2L, players));
        System.out.println(getPlayerById(4L, players));

        System.out.println("3.1");
        System.out.println(getAverageRating(players));

        System.out.println("3.2");

        List<Player> top7 = topN(players, 5);
        for (Player player : top7) {
            System.out.println(player.getNickname() + " " + player.getRating());
        }
        System.out.println();

        Map<String, Object> statsReport = buildStatsReport(players);
        for(Map.Entry<String, Object> entry : statsReport.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}
