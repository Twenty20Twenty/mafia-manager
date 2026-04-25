package com.mafia.manager.entity;

import com.mafia.manager.entity.enums.UserGender;
import com.mafia.manager.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nickname;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "user_gender")
    private UserGender gender;

    @Column(name = "social_link")
    private String socialLink;

    @Column(name = "is_phantom")
    private Boolean isPhantom;

    @Column(name = "phantom_code")
    private String phantomCode;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "user_role")
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(name = "can_create_club_tournaments")
    private Boolean canCreateClubTournaments;

    /** Подтверждён ли email. FALSE у обычных пользователей до подтверждения кода. */
    @Column(name = "is_email_verified")
    private Boolean isEmailVerified;

    public boolean hasRightToCreateTournament() {
        if (this.role == UserRole.admin) return true;
        if (this.club == null) return false;
        if (!Boolean.TRUE.equals(this.club.getIsTournamentOperator())) return false;

        boolean isPresident = this.club.getPresident() != null
                && this.club.getPresident().getId().equals(this.getId());
        return isPresident || Boolean.TRUE.equals(this.canCreateClubTournaments);
    }
}
