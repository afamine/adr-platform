package com.adrplatform.vote.controller;

import com.adrplatform.auth.domain.User;
import com.adrplatform.vote.dto.CastVoteRequest;
import com.adrplatform.vote.dto.VoteDto;
import com.adrplatform.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adrs/{adrId}/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "Cast a vote on an ADR")
    @ApiResponse(responseCode = "201", description = "Vote created")
    @ApiResponse(responseCode = "400", description = "Invalid vote", content = @Content)
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @ApiResponse(responseCode = "404", description = "ADR not found", content = @Content)
    @PostMapping
    public ResponseEntity<VoteDto> castVote(@PathVariable UUID adrId,
                                            @Valid @RequestBody CastVoteRequest request) {
        User voter = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        VoteDto created = voteService.castVote(adrId, request, voter.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get all votes for an ADR")
    @ApiResponse(responseCode = "200", description = "Votes returned")
    @ApiResponse(responseCode = "404", description = "ADR not found", content = @Content)
    @GetMapping
    public ResponseEntity<List<VoteDto>> getVotes(@PathVariable UUID adrId) {
        return ResponseEntity.ok(voteService.getVotesForAdr(adrId));
    }

    @Operation(summary = "Get the current user's vote for an ADR")
    @ApiResponse(responseCode = "200", description = "Vote returned")
    @ApiResponse(responseCode = "404", description = "Vote not found", content = @Content)
    @GetMapping("/my-vote")
    public ResponseEntity<VoteDto> getMyVote(@PathVariable UUID adrId) {
        User voter = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(voteService.getMyVote(adrId, voter.getId()));
    }
}
