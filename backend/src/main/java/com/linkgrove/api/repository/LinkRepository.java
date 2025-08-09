package com.linkgrove.api.repository;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findByUserOrderByDisplayOrderAsc(User user);
    Page<Link> findByUserOrderByDisplayOrderAsc(User user, Pageable pageable);
    Page<Link> findByUser(User user, Pageable pageable);

    List<Link> findByUserAndIsActiveTrueOrderByDisplayOrderAsc(User user);

    Optional<Link> findByIdAndUser(Long id, User user);
    Optional<Link> findByAlias(String alias);

    @Query("SELECT l FROM Link l WHERE l.user.username = :username AND l.isActive = true ORDER BY l.displayOrder ASC")
    List<Link> findActiveLinksForPublicProfile(@Param("username") String username);

    @Query("SELECT COUNT(l) FROM Link l WHERE l.user = :user AND l.isActive = true")
    long countActiveLinksForUser(@Param("user") User user);

    @Query("SELECT MAX(l.displayOrder) FROM Link l WHERE l.user = :user")
    Integer findMaxDisplayOrderForUser(@Param("user") User user);

    @Query("SELECT l FROM Link l WHERE l.user = :user AND (" +
            ":q IS NULL OR :q = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.url) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%')) )")
    Page<Link> searchUserLinks(@Param("user") User user, @Param("q") String q, Pageable pageable);

    @Query("SELECT l FROM Link l WHERE l.user = :user " +
            "AND (:q IS NULL OR :q = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(l.url) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:active IS NULL OR l.isActive = :active)")
    Page<Link> searchUserLinksNoTags(@Param("user") User user,
                                     @Param("q") String q,
                                     @Param("active") Boolean active,
                                     Pageable pageable);

    @Query("SELECT DISTINCT l FROM Link l JOIN l.tags t WHERE l.user = :user " +
            "AND (:q IS NULL OR :q = '' OR LOWER(l.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(l.url) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:active IS NULL OR l.isActive = :active) " +
            "AND LOWER(t.name) IN (:tagNames)")
    Page<Link> searchUserLinksWithTags(@Param("user") User user,
                                       @Param("q") String q,
                                       @Param("tagNames") java.util.List<String> tagNames,
                                       @Param("active") Boolean active,
                                       Pageable pageable);

    @Query("SELECT l FROM Link l WHERE l.user = :user AND (:active IS NULL OR l.isActive = :active)")
    Page<Link> findByUserNoTags(@Param("user") User user,
                                @Param("active") Boolean active,
                                Pageable pageable);

    @Query("SELECT DISTINCT l FROM Link l JOIN l.tags t WHERE l.user = :user AND (:active IS NULL OR l.isActive = :active) AND LOWER(t.name) IN (:tagNames)")
    Page<Link> findByUserWithTags(@Param("user") User user,
                                   @Param("tagNames") java.util.List<String> tagNames,
                                   @Param("active") Boolean active,
                                   Pageable pageable);
}
