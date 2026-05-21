package com.ssafy.codemaestro.domain.board.controller;

import com.ssafy.codemaestro.domain.board.dto.BoardResponseDto;
import com.ssafy.codemaestro.domain.board.dto.CommentRequestDto;
import com.ssafy.codemaestro.domain.board.dto.CommentResponseDto;
import com.ssafy.codemaestro.domain.board.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 게시글별 댓글 조회
    @GetMapping("/{boardId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentByBoardId(@PathVariable Long boardId) {
        return ResponseEntity.ok(commentService.getCommentsByBoardId(boardId));
    }

    // 댓글 생성
    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(requestDto));
    }

    // 댓글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
