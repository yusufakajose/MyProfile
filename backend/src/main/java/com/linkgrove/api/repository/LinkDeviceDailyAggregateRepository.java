package com.linkgrove.api.repository;

import com.linkgrove.api.model.LinkDeviceDailyAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LinkDeviceDailyAggregateRepository extends JpaRepository<LinkDeviceDailyAggregate, Long> {

    @Modifying
    @Query(value = "INSERT INTO link_device_daily_aggregate (username, link_id, day, device_type, clicks, unique_visitors) " +
            "VALUES (:username, :linkId, :day, :device, 1, 0) " +
            "ON CONFLICT (username, link_id, day, device_type) DO UPDATE SET clicks = link_device_daily_aggregate.clicks + 1", nativeQuery = true)
    void upsertIncrement(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("device") String device);

    @Modifying
    @Query(value = "UPDATE link_device_daily_aggregate SET unique_visitors = unique_visitors + 1 WHERE username = :username AND link_id = :linkId AND day = :day AND device_type = :device", nativeQuery = true)
    void incrementUnique(@Param("username") String username,
                         @Param("linkId") Long linkId,
                         @Param("day") LocalDate day,
                         @Param("device") String device);

    @Query("SELECT d FROM LinkDeviceDailyAggregate d WHERE d.username = :username AND d.day BETWEEN :start AND :end")
    List<LinkDeviceDailyAggregate> findRange(@Param("username") String username,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);
}


