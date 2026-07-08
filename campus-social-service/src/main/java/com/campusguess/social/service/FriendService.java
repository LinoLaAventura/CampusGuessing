package com.campusguess.social.service;

import com.campusguess.social.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FriendService {
    FriendshipResponse addFriend(String username, AddFriendRequest request);
    FriendshipResponse handleFriendRequest(String username, String friendName, HandleFriendRequest request);
    FriendListResponse getFriendList(String username, Pageable pageable);
    FriendListResponse getPendingRequests(String username, Pageable pageable);
    FriendListResponse getSentRequests(String username, Pageable pageable);
    boolean isFriend(String username, String friendUsername);
    void removeFriend(String username, String friendUsername);
    void cancelFriendRequest(String username, Long friendshipId);
    Long getFriendCount(String username);
    List<Long> getFriendIds(String username);
}