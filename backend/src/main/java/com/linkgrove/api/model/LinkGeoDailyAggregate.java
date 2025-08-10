package com.linkgrove.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "link_geo_daily_aggregate", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_link_day_country", columnNames = {"username", "link_id", "day", "country"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkGeoDailyAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "country", nullable = false, length = 2)
    private String country; // ISO 3166-1 alpha-2

    @Column(name = "clicks", nullable = false)
    private long clicks;

    @Column(name = "unique_visitors", nullable = false)
    private long uniqueVisitors;
}


