package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkReferrerDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkReferrerDailyAggregateRepository extends JpaRepository<LinkReferrerDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_referrer_daily_aggregate (username, link_id, day, referrer_domain, clicks, unique_visitors) " +
            "VALUES (:username, :linkId, :day, :ref, 1, 0) " +
            "ON CONFLICT (username, link_id, day, referrer_domain) DO UPDATE SET clicks = link_referrer_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("ref") String referrerDomain);

    @Modifying
    @Query(value = "UPDATE link_referrer_daily_aggregate SET unique_visitors = unique_visitors + 1 WHERE username = :username AND link_id = :linkId AND day = :day AND referrer_domain = :ref", nativeQuery = true)
    void incrementUnique(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("ref") String referrerDomain);

    @Query("SELECT r FROM LinkReferrerDailyAggregate r WHERE r.username = :username AND r.day BETWEEN :start AND :end")
    List<LinkReferrerDailyAggregate> findRange(@Param("username") String username,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);
}


