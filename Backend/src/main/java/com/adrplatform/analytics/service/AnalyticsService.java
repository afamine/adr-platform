package com.adrplatform.analytics.service;

import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.analytics.dto.KpiDto;
import com.adrplatform.analytics.dto.StatusCountDto;
import com.adrplatform.analytics.dto.WeeklyActivityDto;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final WorkspaceRepository workspaceRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public KpiDto getKpis(String timeRange) {
        UUID wsId = tenantContext.getWorkspaceId();
        ResolvedRange range = resolveRange(timeRange);

        long total = nullSafe(adrRepository.countCreatedBetween(wsId, range.from(), range.to()));
        long accepted = nullSafe(adrRepository.countByStatusUpdatedBetween(
                wsId, AdrStatus.ACCEPTED, range.from(), range.to()));
        long rejected = nullSafe(adrRepository.countByStatusUpdatedBetween(
                wsId, AdrStatus.REJECTED, range.from(), range.to()));
        long closed = accepted + rejected;
        double acceptanceRate = closed == 0 ? 0.0 : round1((accepted * 100.0) / closed);

        Double rawAvg = adrRepository.avgReviewTimeDaysBetween(wsId, range.from(), range.to());
        double avgReviewDays = rawAvg != null ? round1(rawAvg) : 0.0;

        Instant previousFrom = range.from().minus(range.duration());
        Double avgPrevious = adrRepository.avgReviewTimeDaysBetween(wsId, previousFrom, range.from());
        Double delta = (rawAvg != null && avgPrevious != null) ? round1(rawAvg - avgPrevious) : null;

        long pending = nullSafe(adrRepository.countByStatusCreatedBetween(
                wsId, AdrStatus.UNDER_REVIEW, range.from(), range.to()));
        long pendingApprover = nullSafe(voteRepository.countUnderReviewWithNoApproverVoteCreatedBetween(
                wsId, range.from(), range.to()));

        return new KpiDto(total, total, acceptanceRate,
                accepted, rejected, avgReviewDays, delta, pending, pendingApprover);
    }

    @Transactional(readOnly = true)
    public List<StatusCountDto> getStatusDistribution(String timeRange) {
        UUID wsId = tenantContext.getWorkspaceId();
        ResolvedRange range = resolveRange(timeRange);
        List<Object[]> rows = adrRepository.countGroupByStatusCreatedBetween(wsId, range.from(), range.to());

        Map<String, Long> countMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((AdrStatus) r[0]).name(),
                        r -> (Long) r[1]));

        return Arrays.stream(AdrStatus.values())
                .map(s -> new StatusCountDto(s.name(), countMap.getOrDefault(s.name(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WeeklyActivityDto> getActivity(String timeRange) {
        ResolvedRange range = resolveRange(timeRange);
        if ("24h".equals(range.key())) {
            return getHourlyActivity(range);
        }
        if ("7d".equals(range.key())) {
            return getDailyActivity(range, 7);
        }
        if ("all".equals(range.key())) {
            return getMonthlyActivity(range);
        }
        return getWeeklyActivity(range);
    }

    private List<WeeklyActivityDto> getHourlyActivity(ResolvedRange range) {
        UUID wsId = tenantContext.getWorkspaceId();
        List<WeeklyActivityDto> result = new ArrayList<>();
        LocalDateTime currentHour = LocalDateTime.now(ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 23; i >= 0; i--) {
            LocalDateTime bucketStartTime = currentHour.minusHours(i);
            Instant from = bucketStartTime.toInstant(ZoneOffset.UTC);
            Instant to = bucketStartTime.plusHours(1).minusNanos(1).toInstant(ZoneOffset.UTC);
            long count = nullSafe(adrRepository.countCreatedBetween(wsId, from, minInstant(to, range.to())));
            String label = labelFormatter.format(bucketStartTime);
            result.add(new WeeklyActivityDto(label, label, count));
        }
        return result;
    }

    private List<WeeklyActivityDto> getDailyActivity(ResolvedRange range, int days) {
        UUID wsId = tenantContext.getWorkspaceId();
        List<WeeklyActivityDto> result = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            Instant from = day.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = day.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
            long count = nullSafe(adrRepository.countCreatedBetween(wsId, from, to));
            String label = labelFormatter.format(day);
            result.add(new WeeklyActivityDto(label, label, count));
        }
        return result;
    }

    private List<WeeklyActivityDto> getWeeklyActivity(ResolvedRange range) {
        UUID wsId = tenantContext.getWorkspaceId();
        List<WeeklyActivityDto> result = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d");
        Instant cursor = range.from();
        int index = 1;

        while (cursor.isBefore(range.to())) {
            Instant next = minInstant(cursor.plus(Duration.ofDays(7)), range.to());
            long count = nullSafe(adrRepository.countCreatedBetween(wsId, cursor, next));
            String label = labelFormatter.format(cursor.atZone(ZoneOffset.UTC))
                    + " - "
                    + labelFormatter.format(next.atZone(ZoneOffset.UTC));
            result.add(new WeeklyActivityDto("W" + index, label, count));
            cursor = next;
            index++;
        }
        return result;
    }

    private List<WeeklyActivityDto> getMonthlyActivity(ResolvedRange range) {
        UUID wsId = tenantContext.getWorkspaceId();
        List<WeeklyActivityDto> result = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        Instant cursor = range.from();

        while (cursor.isBefore(range.to())) {
            LocalDate bucketDate = cursor.atZone(ZoneOffset.UTC).toLocalDate();
            Instant nextMonth = bucketDate.withDayOfMonth(1)
                    .plusMonths(1)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            Instant next = minInstant(nextMonth, range.to());
            long count = nullSafe(adrRepository.countCreatedBetween(wsId, cursor, next));
            String label = labelFormatter.format(cursor.atZone(ZoneOffset.UTC));
            result.add(new WeeklyActivityDto(label, label, count));
            cursor = next;
        }
        return result;
    }

    private ResolvedRange resolveRange(String rawRange) {
        String key = rawRange == null || rawRange.isBlank() ? "30d" : rawRange.trim().toLowerCase();
        Instant to = Instant.now();
        if ("all".equals(key)) {
            Instant from = workspaceRepository.findById(tenantContext.getWorkspaceId())
                    .orElseThrow(() -> new BadRequestException("Workspace not found."))
                    .getCreatedAt();
            return new ResolvedRange(key, from, to, Duration.between(from, to));
        }
        Duration duration = switch (key) {
            case "24h" -> Duration.ofHours(24);
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            case "90d" -> Duration.ofDays(90);
            default -> throw new BadRequestException("Unsupported analytics timeRange. Use 24h, 7d, 30d, 90d, or all.");
        };
        return new ResolvedRange(key, to.minus(duration), to, duration);
    }

    private Instant minInstant(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private long nullSafe(Long value) {
        return value != null ? value : 0L;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private record ResolvedRange(String key, Instant from, Instant to, Duration duration) {
    }
}
