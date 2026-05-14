export interface KpiResponse {
  totalAdrs: number;
  totalAdrsThisMonth: number;
  acceptanceRate: number;
  acceptedCount: number;
  rejectedCount: number;
  avgReviewTimeDays: number;
  avgReviewTimeDelta: number | null;
  pendingVotes: number;
  pendingApproverDecisions: number;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface WeeklyActivity {
  week: string;
  label: string;
  count: number;
}

export interface RecentAdrDto {
  id: string;
  adrNumber: number;
  title: string;
  status: string;
  authorName: string;
  lastAction?: string | null;
  lastActionDate?: string | null;
}
