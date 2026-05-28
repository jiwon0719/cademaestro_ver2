package com.ssafy.codemaestro.domain.notification.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {
    @Getter
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long TIMEOUT = 60 * 60 * 1000L;
    private static final String LOST_DATA_KEY_PREFIX = "lost-notifications:";
    private static final long LOST_DATA_TTL_DAYS = 7;

    // 구독
    public SseEmitter subscribe(Long userId) {
        // 2. 이전 연결이 있다면 정리
        emitters.remove(userId);

        // 3. 새로운 emitter 생성
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 4. 이벤트 핸들러 설정
        emitter.onCompletion(() -> {
            SseEmitter existingEmitter = emitters.remove(userId);
            if (existingEmitter != null) {
                try {
                    existingEmitter.complete();
                } catch (Exception e) {
                    log.warn("Emitter complete 호출 중 오류 발생: {}", userId, e);
                }
            }
        });

        emitter.onTimeout(() -> {
            log.info("SSE timeout 실행으로 emiiter 제거됨: {}", userId);
            emitters.remove(userId);

        });

        try {
            // 4. Redis에서 미수신 데이터 꺼내서 전송
            String key = LOST_DATA_KEY_PREFIX + userId;
            List<Object> lostData = redisTemplate.opsForList().range(key, 0, -1);
            redisTemplate.delete(key);

            if (lostData != null && !lostData.isEmpty()) {
                for (Object data : lostData) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("lost-data")
                                .data(data));
                    } catch (IOException e) {
                        log.error("Failed to send lost data to userId: {}", userId, e);
                        saveToLostData(userId, data);
                    }
                }
            }

            // 6. 연결 확인 메시지 전송
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected!"));

            // 7. 모든 데이터 전송 후 최종적으로 emitter 저장
            emitters.put(userId, emitter);
        } catch (IOException e) {
            log.error("SSE 연결 실패: ", e);
            emitters.remove(userId);
            throw new RuntimeException("SSE 연결 실패: ", e);
        }

        log.info("-- SSE Service 연결 완료 for userId: {} --", userId);
        return emitter;
    }

    // 구독 종료
    public void unsubscribe(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
        }
    }

    public void sendNotification(Long userId, String eventType, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) { // 연결된 emitter가 있으면 즉시 전송
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(data));
                log.debug("Successfully sent notification to user {}", userId);

            } catch (IOException e) {
                log.error("Failed to send notification to user {}", userId, e);
                saveToLostData(userId, data);
                emitters.remove(userId); // 문제가 있는 연결 제거
            }
        } else { // 연결된 emitters가 없는 경우 -> 미수신 데이터로 저장되어야 함.
            saveToLostData(userId, data);
            log.debug("User {} is offline, notification saved to lost data : {} ", userId, data);

        }
    }

    private void saveToLostData(Long userId, Object data) {
        String key = LOST_DATA_KEY_PREFIX + userId;
        redisTemplate.opsForList().rightPush(key, data);
        redisTemplate.expire(key, LOST_DATA_TTL_DAYS, TimeUnit.DAYS);
        log.debug("lost data saved to Redis : userId={}", userId);
    }

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void checkConnection() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(""));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }
}