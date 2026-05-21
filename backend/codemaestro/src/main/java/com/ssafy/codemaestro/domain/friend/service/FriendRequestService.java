package com.ssafy.codemaestro.domain.friend.service;

import com.ssafy.codemaestro.domain.friend.dto.FriendListResponseDto;
import com.ssafy.codemaestro.domain.friend.dto.FriendRequestDto;
import com.ssafy.codemaestro.domain.friend.dto.FriendResponseDto;
import com.ssafy.codemaestro.domain.friend.repository.FriendRequestRepository;
import com.ssafy.codemaestro.domain.notification.service.NotificationService;
import com.ssafy.codemaestro.global.entity.User;
import com.ssafy.codemaestro.domain.user.repository.UserRepository;
import com.ssafy.codemaestro.global.entity.FriendRequest;
import com.ssafy.codemaestro.global.entity.FriendRequestStatus;
import com.ssafy.codemaestro.global.exception.AlreadyRequestExistExceptions;
import com.ssafy.codemaestro.global.exception.BadRequestException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FriendRequestService {
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 친구 요청
    public void sendFriendRequest(FriendRequestDto request) {
        FriendRequest friendRequest = saveFriendRequest(request);

        // 알림 전송
        notificationService.sendFriendRequestNotification(
                request.getReceiverId(),
                FriendResponseDto.from(friendRequest, friendRequest.getSender(), friendRequest.getReceiver())
        );
    }

    // 친구 요청 DB 저장
    private FriendRequest saveFriendRequest(FriendRequestDto request) {
        // User 엔티티 조회
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found"));

        // 이미 존재하는 친구 요청이 있는지 확인
        boolean alreadyExists = friendRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                request.getSenderId(), request.getReceiverId(), FriendRequestStatus.PENDING);

        if (alreadyExists) {
            throw new AlreadyRequestExistExceptions("Friend request already exists");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .build();
        return friendRequestRepository.save(friendRequest);
    }

    // 친구 요청 수락
    public void acceptFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Friend request not found"));


        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("이미 처리된 친구 요청입니다.");
        }

        request.accept(); // PENDING -> ACCEPTED
        friendRequestRepository.save(request); // DB 저장

    }

    // 친구 요청 거절
    public void rejectFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Friend request not found"));

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("이미 처리된 친구 요청입니다.");
        }

        request.reject();
        friendRequestRepository.save(request);
    }

    // 대기 중인 요청 조회
    public List<FriendListResponseDto> getPendingRequests(Long userId) {
        return friendRequestRepository
                .findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(fr -> new FriendListResponseDto(
                        fr.getSender().getId(),
                        fr.getSender().getNickname(),
                        fr.getSender().getProfileImageUrl(),
                        fr.getId()
                ))
                .collect(Collectors.toList());
    }

    // 친구 전체 목록 조회
    public List<FriendListResponseDto> getAllFriends(Long userId) {
        return friendRequestRepository
                .findAllFriendsByUserIdAndStatus(userId, FriendRequestStatus.ACCEPTED)
                .stream()
                .map(fr -> {
                    User friend = fr.getSender().getId().equals(userId) ? fr.getReceiver() : fr.getSender();
                    return new FriendListResponseDto(
                            friend.getId(),
                            friend.getNickname(),
                            friend.getProfileImageUrl(),
                            fr.getId()
                    );
                })
                .collect(Collectors.toList());
    }

    // 친구 삭제
    public void deleteFriend(Long requestId) {
        // friendRequestId로 친구 요청을 찾음
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend relationship not found"));

        // ACCEPTED 상태인 경우에만 삭제 가능
        if (request.getStatus() != FriendRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Can only delete accepted friend relationships");
        }

        // 데이터베이스에서 실제로 삭제
        friendRequestRepository.delete(request);
    }
}