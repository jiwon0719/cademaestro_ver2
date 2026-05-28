package com.ssafy.codemaestro.domain.boj.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.codemaestro.domain.boj.dto.BojUserDto;
import com.ssafy.codemaestro.domain.boj.dto.BojUserResponse;
import com.ssafy.codemaestro.domain.user.repository.UserRepository;
import com.ssafy.codemaestro.global.entity.BojUser;
import com.ssafy.codemaestro.domain.boj.repository.BojUserRepository;
import com.ssafy.codemaestro.global.entity.User;
import com.ssafy.codemaestro.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BojService {

    private final WebClient solvedAcWebClient;
    private final BojUserRepository bojUserRepository;
    private final UserRepository userRepository;

    private static final String SOLVED_AC_API_URL = "https://solved.ac/api/v3";
    private static final Duration CACHE_DURATION = Duration.ofHours(1); // 캐시 갱신 주기 : 1시간

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 티어 정보 조회
    @Cacheable(value = "bojUser", key = "#bojId")
    @Transactional
    public BojUserResponse getTierInfo(Long userId, String bojId) {
        BojUser bojUser = bojUserRepository.findByHandle(bojId)
                .orElseGet(() -> updateBojUserInfo(userId, bojId));

        return convertToResponse(bojUser);
    }

    // 티어 정보 수정 및 등록
    // private -> public으로 수정한 이유
    // Redis : Spring AOP 기반으로 동작하는데, private 메서드에는 AOP가 적용 안됨
    // Spring AOP는 프록시 패턴을 사용하는데 private 메서드는 프록시가 가로챌 수 없음
    /**
     *
     * @param userId
     * @param bojId
     * @return
     */
    @CacheEvict(value = "bojUser", key = "#bojId")
    public BojUser updateBojUserInfo(Long userId, String bojId) {
        BojUser bojUser = bojUserRepository.findByUserId(userId)
                .map(existing -> {
                    existing.updateHandle(bojId);
                    return existing;
                })
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("User not Found"));
                    return BojUser.builder()
                            .user(user)
                            .handle(bojId)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                });

        // API 호출 먼저
        BojUserDto userInfo = fetchUserInfoFromSolvedAc(bojId);

        // tier 정보 업데이트 후 한 번만 저장
        bojUser.updateTierInfo(userInfo.getTier());
        return bojUserRepository.save(bojUser);
    }

    // solved.ac.API 티어 받아오기
    public BojUserDto fetchUserInfoFromSolvedAc(String bojId) {
        return solvedAcWebClient.get()
                .uri(SOLVED_AC_API_URL + "/user/show?handle={bojId}", bojId)
                .retrieve() // HTTP 응답 받기
                .bodyToMono(String.class)  // 응답 -> String으로 반환
                .map(response -> {
                    // 변환 실패시 RuntimeException 발생
                    try {
                        log.debug("solved.ac API 응답: {}", response);
                        return objectMapper.readValue(response, BojUserDto.class);
                    } catch (JsonProcessingException e) {
                        log.error("JSON 파싱 에러: {}", e.getMessage(), e);
                        throw new RuntimeException("JSON 파싱 실패", e);
                    }
                })
                .block(); // 비동기 처리가 완료될 때까지 대기
    }

    // 엔티티 -> DTO 변환
    private BojUserResponse convertToResponse(BojUser bojUser) {
        return BojUserResponse.builder()
                .handle(bojUser.getHandle())
                .tier(bojUser.getTier())
                .build();
    }
}