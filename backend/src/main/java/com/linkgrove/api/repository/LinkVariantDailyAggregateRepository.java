package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkVariantDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkVariantDailyAggregateRepository extends JpaRepository<LinkVariantDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_variant_daily_aggregate (username, link_id, variant_id, day, clicks, unique_visitors) " +
            "VALUES (:username, :linkId, :variantId, :day, 1, 0) " +
            "ON CONFLICT (username, link_id, variant_id, day) DO UPDATE SET clicks = link_variant_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                          @Param("linkId") Long linkId,
                          @Param("variantId") Long variantId,
                          @Param("day") LocalDate day);

    @Modifying
    @Query(value = "UPDATE link_variant_daily_aggregate SET unique_visitors = unique_visitors + 1 " +
            "WHERE username = :username AND link_id = :linkId AND variant_id = :variantId AND day = :day", nativeQuery = true)
    void incrementUnique(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("variantId") Long variantId,
                         @Param("day") LocalDate day);

    @Query("SELECT a FROM LinkVariantDailyAggregate a WHERE a.username = :username AND a.day BETWEEN :start AND :end")
    List<LinkVariantDailyAggregate> findRange(@Param("username") String username,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);
}


