package com.adrplatform.analytics.service;

import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.analytics.dto.KpiDto;
import com.adrplatform.analytics.dto.StatusCountDto;
import com.adrplatform.analytics.dto.WeeklyActivityDto;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AdrRepository adrRepository;
    private final VoteRepository voteRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public KpiDto getKpis() {
        UUID wsId = tenantContext.getWorkspaceId();

        long total = nullSafe(adrRepository.countByWorkspace(wsId));

        Instant firstOfMonth = LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long thisMonth = nullSafe(adrRepository.countSince(wsId, firstOfMonth));

        long accepted = nullSafe(adrRepository.countByStatus(wsId, AdrStatus.ACCEPTED));
        long rejected = nullSafe(adrRepository.countByStatus(wsId, AdrStatus.REJECTED));
        long closed = accepted + rejected;
        double acceptanceRate = closed == 0 ? 0.0 : round1((accepted * 100.0) / closed);

        Double rawAvg = adrRepository.avgReviewTimeDays(wsId);
        double avgReviewDays = rawAvg != null ? round1(rawAvg) : 0.0;

        Instant now = Instant.now();
        Instant prevMonthStart = LocalDate.now(ZoneOffset.UTC)
                .minusMonths(1).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant prevMonthEnd = firstOfMonth.minusSeconds(1);
        Double avgThisMonth = adrRepository.avgReviewTimeDaysBetween(wsId, firstOfMonth, now);
        Double avgPrevMonth = adrRepository.avgReviewTimeDaysBetween(wsId, prevMonthStart, prevMonthEnd);
        Double delta = (avgThisMonth != null && avgPrevMonth != null)
                ? round1(avgThisMonth - avgPrevMonth) : null;

        long pending = nullSafe(adrRepository.countByStatus(wsId, AdrStatus.UNDER_REVIEW));
        long pendingApprover = nullSafe(voteRepository.countUnderReviewWithNoApproverVote(wsId));

        return new KpiDto(total, thisMonth, acceptanceRate,
                accepted, rejected, avgReviewDays, delta, pending, pendingApprover);
    }

    @Transactional(readOnly = true)
    public List<StatusCountDto> getStatusDistribution() {
        UUID wsId = tenantContext.getWorkspaceId();
        List<Object[]> rows = adrRepository.countGroupByStatus(wsId);

        Map<String, Long> countMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((AdrStatus) r[0]).name(),
                        r -> (Long) r[1]));

        return Arrays.stream(AdrStatus.values())
                .map(s -> new StatusCountDto(s.name(), countMap.getOrDefault(s.name(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WeeklyActivityDto> getWeeklyActivity(int weeks) {
        UUID wsId = tenantContext.getWorkspaceId();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
        List<WeeklyActivityDto> result = new ArrayList<>();

        for (int i = 0; i < weeks; i++) {
            LocalDate weekStart = LocalDate.now(ZoneOffset.UTC)
                    .minusWeeks(weeks - 1 - i)
                    .with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            Instant from = weekStart.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = weekEnd.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

            long count = nullSafe(adrRepository.countCreatedBetween(wsId, from, to));
            String label = fmt.format(weekStart) + " - " + fmt.format(weekEnd);
            result.add(new WeeklyActivityDto("W" + (i + 1), label, count));
        }

        return result;
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
