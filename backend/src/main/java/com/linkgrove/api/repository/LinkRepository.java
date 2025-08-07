package com.linkgrove.api.repository;

import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findByUserOrderByDisplayOrderAsc(User user);

    List<Link> findByUserAndIsActiveTrueOrderByDisplayOrderAsc(User user);

    Optional<Link> findByIdAndUser(Long id, User user);

    @Query("SELECT l FROM Link l WHERE l.user.username = :username AND l.isActive = true ORDER BY l.displayOrder ASC")
    List<Link> findActiveLinksForPublicProfile(@Param("username") String username);

    @Query("SELECT COUNT(l) FROM Link l WHERE l.user = :user AND l.isActive = true")
    long countActiveLinksForUser(@Param("user") User user);

    @Query("SELECT MAX(l.displayOrder) FROM Link l WHERE l.user = :user")
    Integer findMaxDisplayOrderForUser(@Param("user") User user);
}
