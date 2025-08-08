package com.linkgrove.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "link_click_daily_aggregate", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_link_day", columnNames = {"username", "link_id", "day"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkClickDailyAggregate {

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

    @Column(name = "clicks", nullable = false)
    private long clicks;

    @Column(name = "unique_visitors", nullable = false)
    private long uniqueVisitors;
}
