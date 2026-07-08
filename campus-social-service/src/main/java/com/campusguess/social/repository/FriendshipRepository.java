package com.campusguess.social.repository;

import com.campusguess.social.entity.Friendship;
import com.campusguess.common.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.sender = :user1 AND f.receiver = :user2) OR " +
           "(f.sender = :user2 AND f.receiver = :user1)")
    List<Friendship> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    Optional<Friendship> findBySenderAndReceiverAndStatus(User sender, User receiver, Friendship.FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE (f.sender = :user OR f.receiver = :user) AND f.status = 'APPROVED'")
    Page<Friendship> findApprovedFriendships(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE f.receiver = :user AND f.status = 'PENDING'")
    Page<Friendship> findPendingRequests(@Param("user") User user, Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE f.sender = :user AND f.status = 'PENDING'")
    Page<Friendship> findSentRequests(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE f.status = 'APPROVED' AND " +
           "((f.sender = :user1 AND f.receiver = :user2) OR (f.sender = :user2 AND f.receiver = :user1))")
    boolean existsApprovedFriendship(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT f.receiver.id FROM Friendship f WHERE f.sender.id = :userId AND f.status = 'APPROVED'")
    List<Long> findFriendIdsBySenderId(@Param("userId") Long userId);

    @Query("SELECT f.sender.id FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'APPROVED'")
    List<Long> findFriendIdsByReceiverId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.status = 'APPROVED' AND " +
           "(f.sender = :user OR f.receiver = :user)")
    Long countApprovedFriendships(@Param("user") User user);
}