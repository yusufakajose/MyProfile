package com.linkgrove.api.repository;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.LinkVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LinkVariantRepository extends JpaRepository<LinkVariant, Long> {

    @Query("select v from LinkVariant v where v.link = :link and v.isActive = true and v.weight > 0")
    List<LinkVariant> findActiveByLink(@Param("link") Link link);
}


