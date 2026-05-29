export type AdrStatus = 'DRAFT' | 'PROPOSED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'SUPERSEDED';
export type Role = 'AUTHOR' | 'REVIEWER' | 'APPROVER' | 'ADMIN';
export type AdrStatusFilter = AdrStatus | 'ALL';
export type AdrTabKey = 'context' | 'decision' | 'consequences' | 'alternatives';

export interface AdrDto {
  id: string;
  adrNumber: number;
  title: string;
  status: AdrStatus;
  supersededById?: string | null;
  supersededByAdrNumber?: number | null;
  supersededByTitle?: string | null;
  supersedesId?: string | null;
  supersedesAdrNumber?: number | null;
  supersedesTitle?: string | null;
  context?: string;
  decision?: string;
  consequences?: string;
  alternatives?: string;
  tags: string[];
  authorId: string;
  authorName: string;
  createdAt: string;
  updatedAt: string;
  workspaceId: string;
}

export interface VoteDto {
  id: string;
  adrId: string;
  voterId: string;
  voterName: string;
  voterRole: 'REVIEWER' | 'APPROVER' | 'ADMIN';
  voteType: VoteType;
  comment: string;
  createdAt: string;
}

export type VoteType = 'APPROVE' | 'REJECT';

export interface CastVoteRequest {
  voteType: VoteType;
  comment: string;
}

export type AuditEventType =
  'STATUS_CHANGED' | 'ADR_CREATED' | 'ADR_UPDATED' |
  'VOTE_CAST' | 'COMMENT_ADDED' | (string & {});

export interface AuditEventDto {
  id: string;
  type: AuditEventType;
  actor: string | null;
  action: string;
  timestamp: string;
  detail?: string | null;
  payload?: Record<string, unknown> | null;
}

export interface AiInsight {
  title: string;
  confidence: number;
  impact: 'high' | 'medium' | 'low';
  description: string;
  rationale: string;
  source?: string;
}

export interface CreateAdrRequest {
  title: string;
  context?: string;
  decision?: string;
  consequences?: string;
  alternatives?: string;
  tags?: string[];
}

export interface UpdateAdrRequest {
  title?: string;
  context?: string;
  decision?: string;
  consequences?: string;
  alternatives?: string;
  tags?: string[];
}

export interface StatusTransitionRequest {
  status: AdrStatus;
  supersededByAdrId?: string | null;
}

export interface AdrSummary {
  id: string;
  adrNumber: number;
  title: string;
  status: AdrStatus;
}

export type Adr = AdrDto;

export interface CommentDto {
  id: string;
  adrId: string;
  authorId: string;
  authorName: string;
  authorInitials: string;
  content: string;
  resolved: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HistoryEventDto {
  actor: string;
  action: string;
  timeAgo: string;
  eventType: string;
}

export interface TeamMemberDto {
  id: string;
  fullName: string;
  initials: string;
  role: string;
  avatarColor: string;
}

export const ADR_STATUS_OPTIONS: ReadonlyArray<{ value: AdrStatus; label: string }> = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PROPOSED', label: 'Proposed' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'SUPERSEDED', label: 'Superseded' }
];

export const ADR_FILTER_OPTIONS: ReadonlyArray<{ value: AdrStatusFilter; label: string }> = [
  { value: 'ALL', label: 'All' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PROPOSED', label: 'Proposed' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'SUPERSEDED', label: 'Superseded' }
];

export const ADR_TAB_ORDER: ReadonlyArray<{ key: AdrTabKey; label: string }> = [
  { key: 'context', label: 'Context' },
  { key: 'decision', label: 'Decision' },
  { key: 'consequences', label: 'Consequences' },
  { key: 'alternatives', label: 'Alternatives' }
];

export const ADR_TAB_PROMPTS: Record<AdrTabKey, { question: string; placeholder: string }> = {
  context: {
    question: 'What is the context and background for this decision?',
    placeholder: 'Describe the architectural context, constraints, and requirements that led to this decision...'
  },
  decision: {
    question: 'What decision was made and why?',
    placeholder: 'State the architectural decision made, including the rationale and key considerations...'
  },
  consequences: {
    question: 'What are the consequences of this decision?',
    placeholder: 'Describe the resulting context, including positive and negative consequences...'
  },
  alternatives: {
    question: 'What alternatives were considered?',
    placeholder: 'List the alternatives that were considered and why they were rejected...'
  }
};

export function allowedTransitions(
  adr: AdrDto,
  me: { id: string; role: Role | string }
): AdrStatus[] {
  const role = me.role as string;
  const isOwner = adr.authorId === me.id;
  const isAdmin = role === 'ADMIN';

  if (isAdmin) {
    const adminTransitions = ['DRAFT', 'PROPOSED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'SUPERSEDED']
      .filter((status) => status !== adr.status) as AdrStatus[];

    if (adr.status !== 'ACCEPTED') {
      return adminTransitions.filter((status) => status !== 'SUPERSEDED');
    }

    return adminTransitions;
  }

  switch (adr.status) {
    case 'DRAFT':
      return role === 'AUTHOR' && isOwner ? ['PROPOSED'] : [];
    case 'PROPOSED': {
      const options: AdrStatus[] = [];
      if (role === 'REVIEWER') {
        options.push('UNDER_REVIEW');
      }
      if (role === 'AUTHOR' && isOwner) {
        options.push('DRAFT');
      }
      return options;
    }
    case 'UNDER_REVIEW':
      return role === 'APPROVER' ? ['ACCEPTED', 'REJECTED'] : [];
    case 'ACCEPTED':
      return role === 'APPROVER' ? ['SUPERSEDED'] : [];
    case 'REJECTED':
      return role === 'AUTHOR' && isOwner ? ['DRAFT'] : [];
    default:
      return [];
  }
}

export interface CompletenessResult {
  filledSections: number;
  totalSections: number;
  percentage: number;
  missingSections: string[];
  hasTags: boolean;
}

export function completenessScore(
  adr: Pick<AdrDto, 'context' | 'decision' | 'consequences' | 'alternatives' | 'tags'>
): CompletenessResult {
  const sections: Array<{ key: keyof typeof adr; label: string }> = [
    { key: 'context', label: 'Context' },
    { key: 'decision', label: 'Decision' },
    { key: 'consequences', label: 'Consequences' },
    { key: 'alternatives', label: 'Alternatives' }
  ];

  const missing = sections
    .filter((s) => !((adr[s.key] as string)?.trim()))
    .map((s) => s.label);

  const filled = sections.length - missing.length;
  const hasTags = Array.isArray(adr.tags) && adr.tags.length > 0;

  return {
    filledSections: filled,
    totalSections: sections.length,
    percentage: Math.round((filled / sections.length) * 100),
    missingSections: missing,
    hasTags
  };
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
