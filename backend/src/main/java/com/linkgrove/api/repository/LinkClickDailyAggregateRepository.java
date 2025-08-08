package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkClickDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkClickDailyAggregateRepository extends JpaRepository<LinkClickDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_click_daily_aggregate (username, link_id, day, clicks) " +
            "VALUES (:username, :linkId, :day, 1) " +
            "ON CONFLICT (username, link_id, day) DO UPDATE SET clicks = link_click_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day);

    @Query("SELECT a FROM LinkClickDailyAggregate a WHERE a.username = :username AND a.day BETWEEN :start AND :end")
    List<LinkClickDailyAggregate> findRange(@Param("username") String username,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    @Query("SELECT a FROM LinkClickDailyAggregate a WHERE a.username = :username AND a.link.id = :linkId AND a.day BETWEEN :start AND :end")
    List<LinkClickDailyAggregate> findRangeForLink(@Param("username") String username,
                                                   @Param("linkId") Long linkId,
                                                   @Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);
}
