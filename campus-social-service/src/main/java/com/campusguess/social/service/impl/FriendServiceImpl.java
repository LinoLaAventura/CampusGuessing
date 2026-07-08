package com.campusguess.social.service.impl;

import com.campusguess.common.exception.BusinessException;
import com.campusguess.social.dto.*;
import com.campusguess.social.entity.Friendship;
import com.campusguess.common.entity.User;
import com.campusguess.social.repository.FriendshipRepository;
import com.campusguess.social.repository.UserRepository;
import com.campusguess.social.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFriendIds(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        List<Long> senderIds = friendshipRepository.findFriendIdsBySenderId(user.getId());
        List<Long> receiverIds = friendshipRepository.findFriendIdsByReceiverId(user.getId());

        return Stream.concat(senderIds.stream(), receiverIds.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FriendshipResponse addFriend(String username, AddFriendRequest request) {
        String friendUsername = request.getFriendUsername();

        if (username.equals(friendUsername)) {
            throw new BusinessException(400, "不能添加自己为好友");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        User friendUser = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new BusinessException(404, "目标用户不存在"));

        // 检查我是否已向对方发送申请
        Optional<Friendship> mySentOpt = friendshipRepository.findBySenderAndReceiver(user, friendUser);
        if (mySentOpt.isPresent()) {
            Friendship mySent = mySentOpt.get();
            switch (mySent.getStatus()) {
                case PENDING:
                    throw new BusinessException(400, "已向该用户发送过好友申请，等待对方处理");
                case APPROVED:
                    throw new BusinessException(400, "你们已经是好友了");
                case REJECTED:
                    friendshipRepository.delete(mySent);
                    log.info("用户{}重新向用户{}发送好友申请，删除旧拒绝记录", username, friendUsername);
                    break;
            }
        }

        // 检查对方是否已向我发送申请
        Optional<Friendship> receivedOpt = friendshipRepository.findBySenderAndReceiver(friendUser, user);
        if (receivedOpt.isPresent()) {
            Friendship received = receivedOpt.get();
            if (received.getStatus() == Friendship.FriendshipStatus.PENDING) {
                return acceptExistingRequest(username, friendUsername, received);
            } else if (received.getStatus() == Friendship.FriendshipStatus.APPROVED) {
                throw new BusinessException(400, "你们已经是好友了");
            } else if (received.getStatus() == Friendship.FriendshipStatus.REJECTED) {
                if (friendshipRepository.existsApprovedFriendship(user, friendUser)) {
                    throw new BusinessException(400, "你们已经是好友了");
                }
            }
        }

        if (friendshipRepository.existsApprovedFriendship(user, friendUser)) {
            throw new BusinessException(400, "你们已经是好友了");
        }

        Friendship friendship = new Friendship();
        friendship.setSender(user);
        friendship.setReceiver(friendUser);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);

        Friendship saved = friendshipRepository.save(friendship);
        log.info("用户{}向用户{}发送了好友申请，申请ID: {}", username, friendUsername, saved.getId());

        return createFriendshipResponse(saved);
    }

    private FriendshipResponse acceptExistingRequest(String username, String friendUsername, Friendship existingRequest) {
        existingRequest.setStatus(Friendship.FriendshipStatus.APPROVED);
        existingRequest.setHandledAt(LocalDateTime.now());
        existingRequest.setHandledType("accept");
        Friendship savedRequest = friendshipRepository.save(existingRequest);

        User user = existingRequest.getReceiver();
        User friendUser = existingRequest.getSender();

        Optional<Friendship> reverseOpt = friendshipRepository.findBySenderAndReceiver(user, friendUser);
        if (reverseOpt.isPresent()) {
            Friendship reverse = reverseOpt.get();
            reverse.setStatus(Friendship.FriendshipStatus.APPROVED);
            reverse.setHandledAt(LocalDateTime.now());
            reverse.setHandledType("accept");
            friendshipRepository.save(reverse);
            log.info("用户{}接受了用户{}的好友申请，并更新了已有的反向记录", username, friendUsername);
        } else {
            createReverseFriendship(existingRequest);
            log.info("用户{}接受了用户{}的好友申请，并创建了新的反向记录", username, friendUsername);
        }

        log.info("用户{}自动接受了用户{}的好友申请，建立了双向好友关系", username, friendUsername);
        return createFriendshipResponse(savedRequest);
    }

    @Override
    @Transactional
    public FriendshipResponse handleFriendRequest(String username, String friendName, HandleFriendRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        User friendUser = userRepository.findByUsername(friendName)
                .orElseThrow(() -> new BusinessException(404, "好友用户不存在"));

        Friendship friendship = friendshipRepository
                .findBySenderAndReceiver(friendUser, user)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));

        if (!friendship.getReceiver().getUsername().equals(username)) {
            throw new BusinessException(403, "无权处理该好友申请");
        }

        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new BusinessException(400, "该好友申请已处理");
        }

        String handleType = request.getHandleType().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        if ("accept".equals(handleType)) {
            friendship.setStatus(Friendship.FriendshipStatus.APPROVED);
            friendship.setHandledType("accept");
            friendship.setHandledAt(now);

            Optional<Friendship> reverseOpt = friendshipRepository.findBySenderAndReceiver(user, friendUser);
            if (reverseOpt.isPresent()) {
                Friendship reverse = reverseOpt.get();
                reverse.setStatus(Friendship.FriendshipStatus.APPROVED);
                reverse.setHandledAt(now);
                reverse.setHandledType("accept");
                friendshipRepository.save(reverse);
                log.info("接受好友申请时更新了已有的反向记录");
            } else {
                createReverseFriendship(friendship);
            }

            log.info("用户{}接受了用户{}的好友申请，申请ID: {}", username, friendship.getSender().getId(), friendship.getId());
        } else if ("reject".equals(handleType)) {
            removeFriend(username, friendName);
            friendship.setHandledType("reject");
            friendship.setHandledAt(now);
            log.info("用户{}拒绝了用户{}的好友申请（通过删除关系实现）", username, friendship.getSender().getId());
            return createFriendshipResponse(friendship);
        } else {
            throw new BusinessException(400, "处理类型必须是accept或reject");
        }

        Friendship updated = friendshipRepository.save(friendship);
        return createFriendshipResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public FriendListResponse getFriendList(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        List<Long> senderIds = friendshipRepository.findFriendIdsBySenderId(user.getId());
        List<Long> receiverIds = friendshipRepository.findFriendIdsByReceiverId(user.getId());
        List<Long> friendIds = Stream.concat(senderIds.stream(), receiverIds.stream())
                .distinct()
                .collect(Collectors.toList());

        long total = friendIds.size();

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int start = page * size;
        int end = Math.min(start + size, friendIds.size());

        List<FriendResponse> friendList;
        if (start >= end) {
            friendList = List.of();
        } else {
            List<Long> pageIds = friendIds.subList(start, end);
            friendList = pageIds.stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new BusinessException(404, "好友用户不存在")))
                    .map(friendUser -> new FriendResponse(
                            friendUser.getId(),
                            friendUser.getUsername(),
                            friendUser.getPoints(),
                            Friendship.FriendshipStatus.APPROVED.name().toLowerCase(),
                            friendUser.getLastLoginAt(),
                            null))
                    .toList();
        }

        log.debug("用户{}查询好友列表（去重），共{}条记录", username, total);
        return new FriendListResponse(total, friendList);
    }

    @Override
    @Transactional(readOnly = true)
    public FriendListResponse getPendingRequests(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Page<Friendship> pendingRequests = friendshipRepository.findPendingRequests(user, pageable);

        List<FriendResponse> requestList = pendingRequests.stream()
                .map(friendship -> new FriendResponse(
                        friendship.getSender().getId(),
                        friendship.getSender().getUsername(),
                        friendship.getSender().getPoints(),
                        friendship.getStatus().name().toLowerCase(),
                        friendship.getSender().getLastLoginAt(),
                        friendship.getRequestedAt()))
                .toList();

        log.debug("用户{}查询待处理好友申请，共{}条记录", username, pendingRequests.getTotalElements());
        return new FriendListResponse(pendingRequests.getTotalElements(), requestList);
    }

    @Override
    @Transactional(readOnly = true)
    public FriendListResponse getSentRequests(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Page<Friendship> sentRequests = friendshipRepository.findSentRequests(user, pageable);

        List<FriendResponse> requestList = sentRequests.stream()
                .map(friendship -> new FriendResponse(
                        friendship.getReceiver().getId(),
                        friendship.getReceiver().getUsername(),
                        friendship.getReceiver().getPoints(),
                        friendship.getStatus().name().toLowerCase(),
                        friendship.getReceiver().getLastLoginAt(),
                        friendship.getRequestedAt()))
                .toList();

        log.debug("用户{}查询已发送的好友申请，共{}条记录", username, sentRequests.getTotalElements());
        return new FriendListResponse(sentRequests.getTotalElements(), requestList);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        User friendUser = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new BusinessException(404, "好友用户不存在"));

        boolean isFriend = friendshipRepository.existsApprovedFriendship(user, friendUser);
        log.debug("检查用户{}和用户{}是否是好友：{}", username, friendUsername, isFriend);
        return isFriend;
    }

    @Override
    @Transactional
    public void removeFriend(String username, String friendUsername) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        User friendUser = userRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new BusinessException(404, "好友用户不存在"));

        List<Friendship> friendships = friendshipRepository.findBetweenUsers(user, friendUser);

        if (friendships.isEmpty()) {
            throw new BusinessException(404, "好友关系不存在");
        }

        friendshipRepository.deleteAll(friendships);
        log.info("用户{}删除了好友{}，删除了{}条关系记录", username, friendUsername, friendships.size());
    }

    @Override
    @Transactional
    public void cancelFriendRequest(String username, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new BusinessException(404, "好友申请不存在"));

        if (!friendship.getSender().getUsername().equals(username)) {
            throw new BusinessException(403, "只能取消自己发送的好友申请");
        }

        if (friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new BusinessException(400, "只能取消待处理的好友申请");
        }

        friendshipRepository.delete(friendship);
        log.info("用户{}取消了好友申请ID: {}", username, friendshipId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getFriendCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        Long count = friendshipRepository.countApprovedFriendships(user);
        log.debug("用户{}的好友数量：{}", username, count);
        return count;
    }

    private FriendshipResponse createFriendshipResponse(Friendship friendship) {
        FriendshipResponse response = new FriendshipResponse();
        response.setFriendshipId(friendship.getId());
        response.setApplicantId(friendship.getSender().getId());
        response.setApplicantUsername(friendship.getSender().getUsername());
        response.setReceiverId(friendship.getReceiver().getId());
        response.setReceiverUsername(friendship.getReceiver().getUsername());
        response.setStatus(friendship.getStatus().name().toLowerCase());
        response.setRequestedAt(friendship.getRequestedAt());

        if (friendship.getHandledAt() != null) {
            response.setHandledAt(friendship.getHandledAt());
            response.setHandledType(friendship.getHandledType());
        }

        return response;
    }

    private void createReverseFriendship(Friendship originalFriendship) {
        Friendship reverseFriendship = new Friendship();
        reverseFriendship.setSender(originalFriendship.getReceiver());
        reverseFriendship.setReceiver(originalFriendship.getSender());
        reverseFriendship.setStatus(Friendship.FriendshipStatus.APPROVED);
        reverseFriendship.setHandledAt(LocalDateTime.now());
        reverseFriendship.setHandledType("accept");

        friendshipRepository.save(reverseFriendship);

        log.info("创建反向好友关系：用户{}到用户{}",
                originalFriendship.getReceiver().getId(),
                originalFriendship.getSender().getId());
    }
}