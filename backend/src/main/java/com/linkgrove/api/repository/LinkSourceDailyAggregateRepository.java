package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkSourceDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkSourceDailyAggregateRepository extends JpaRepository<LinkSourceDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_source_daily_aggregate (username, link_id, day, source, clicks, unique_visitors) " +
            "VALUES (:username, :linkId, :day, :src, 1, 0) " +
            "ON CONFLICT (username, link_id, day, source) DO UPDATE SET clicks = link_source_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("src") String source);

    @Modifying
    @Query(value = "UPDATE link_source_daily_aggregate SET unique_visitors = unique_visitors + 1 WHERE username = :username AND link_id = :linkId AND day = :day AND source = :src", nativeQuery = true)
    void incrementUnique(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("src") String source);

    @Query("SELECT s FROM LinkSourceDailyAggregate s WHERE s.username = :username AND s.day BETWEEN :start AND :end")
    List<LinkSourceDailyAggregate> findRange(@Param("username") String username,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    @Query("SELECT s FROM LinkSourceDailyAggregate s WHERE s.username = :username AND s.link.id = :linkId AND s.day BETWEEN :start AND :end")
    List<LinkSourceDailyAggregate> findRangeForLink(@Param("username") String username,
                                                    @Param("linkId") Long linkId,
                                                    @Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);
}


