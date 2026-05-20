package com.adrplatform.adr.controller;

import com.adrplatform.adr.dto.CommentDto;
import com.adrplatform.adr.dto.CreateCommentRequest;
import com.adrplatform.adr.dto.HistoryEventDto;
import com.adrplatform.adr.dto.ResolveCommentRequest;
import com.adrplatform.adr.dto.TeamMemberDto;
import com.adrplatform.adr.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adrs/{id}")
@RequiredArgsConstructor
public class CollaborationController {

    private final CommentService commentService;

    @Operation(summary = "List all comments on an ADR")
    @ApiResponse(responseCode = "200", description = "Comments returned")
    @GetMapping("/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(commentService.getComments(id));
    }

    @Operation(summary = "Add a comment to an ADR")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @PostMapping("/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(id, request));
    }

    @Operation(summary = "Resolve or unresolve a comment")
    @ApiResponse(responseCode = "200", description = "Comment updated")
    @PatchMapping("/comments/{commentId}/resolve")
    public ResponseEntity<CommentDto> resolveComment(
            @PathVariable("id") UUID id,
            @PathVariable("commentId") UUID commentId,
            @RequestBody ResolveCommentRequest request) {
        return ResponseEntity.ok(commentService.resolveComment(id, commentId, request.resolved()));
    }

    @Operation(summary = "Get activity history for an ADR")
    @ApiResponse(responseCode = "200", description = "History returned")
    @GetMapping("/history")
    public ResponseEntity<List<HistoryEventDto>> getHistory(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(commentService.getHistory(id));
    }

    @Operation(summary = "Get team members for the ADR's workspace")
    @ApiResponse(responseCode = "200", description = "Team returned")
    @GetMapping("/team")
    public ResponseEntity<List<TeamMemberDto>> getTeam(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(commentService.getTeam(id));
    }
}
