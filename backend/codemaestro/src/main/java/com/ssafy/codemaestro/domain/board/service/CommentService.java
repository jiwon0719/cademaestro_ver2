package com.ssafy.codemaestro.domain.board.service;

import com.ssafy.codemaestro.domain.board.dto.CommentRequestDto;
import com.ssafy.codemaestro.domain.board.dto.CommentResponseDto;
import com.ssafy.codemaestro.domain.board.repository.BoardRepository;
import com.ssafy.codemaestro.domain.board.repository.CommentRepository;
import com.ssafy.codemaestro.domain.notification.service.NotificationService;
import com.ssafy.codemaestro.domain.user.repository.UserRepository;
import com.ssafy.codemaestro.global.entity.Board;
import com.ssafy.codemaestro.global.entity.Comment;
import com.ssafy.codemaestro.global.entity.User;
import com.ssafy.codemaestro.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    // 게시글 별 댓글 조회
    public List<CommentResponseDto> getCommentsByBoardId(long boardId) {
        List<Comment> comments = commentRepository.findByBoardId(boardId);

        return comments.stream()
                .map(CommentResponseDto::from)
                .collect(Collectors.toList());
    }

    // 댓글 생성
    @Transactional
    public CommentResponseDto createComment(CommentRequestDto commentRequestDto) {
        Board board = boardRepository.findById(commentRequestDto.getBoardId())
                .orElseThrow(() -> new BadRequestException("Board not Found"));
        User user = userRepository.findById(commentRequestDto.getUserId())
                .orElseThrow(() -> new BadRequestException("User not Found"));

        Comment comment = new Comment();
        comment.setContent(commentRequestDto.getContent());
        comment.setBoard(board);
        comment.setWriter(user);

        Comment savedComment = commentRepository.save(comment);

        // Board 작성자에게 알림 전송
        Long boardWriter = board.getWriter().getId();
        notificationService.sendCommentNotification(
                boardWriter,
                CommentResponseDto.from(savedComment)
        );

        return CommentResponseDto.from(savedComment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Comment not Found"));
        commentRepository.delete(comment);
    }
}
