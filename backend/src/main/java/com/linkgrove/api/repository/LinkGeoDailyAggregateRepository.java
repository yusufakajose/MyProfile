package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkGeoDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkGeoDailyAggregateRepository extends JpaRepository<LinkGeoDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_geo_daily_aggregate (username, link_id, day, country, clicks, unique_visitors) " +
            "VALUES (:username, :linkId, :day, :country, 1, 0) " +
            "ON CONFLICT (username, link_id, day, country) DO UPDATE SET clicks = link_geo_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("country") String country);

    @Modifying
    @Query(value = "UPDATE link_geo_daily_aggregate SET unique_visitors = unique_visitors + 1 WHERE username = :username AND link_id = :linkId AND day = :day AND country = :country", nativeQuery = true)
    void incrementUnique(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("country") String country);

    @Query("SELECT g FROM LinkGeoDailyAggregate g WHERE g.username = :username AND g.day BETWEEN :start AND :end")
    List<LinkGeoDailyAggregate> findRange(@Param("username") String username,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}


