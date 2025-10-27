package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentStatus;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getComments(
            @RequestParam(required = false) List<Long> eventIds,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return commentService.getAdminComments(eventIds, statuses, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateCommentStatus(@PathVariable Long commentId,
                                          @RequestParam CommentStatus status) {
        return commentService.updateAdminComment(commentId, status.name());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteAdminComment(commentId);
    }
}