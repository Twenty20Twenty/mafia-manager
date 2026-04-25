package com.mafia.manager.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clubs")
public class Club extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "logo_url")
    private String logoUrl;

    private String description;

    @Column(name = "social_link")
    private String socialLink;

    @Column(name = "is_tournament_operator")
    private Boolean isTournamentOperator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "president_id")
    @ToString.Exclude // Избегаем циклической ссылки при логировании
    private User president;
}
