export type AdrStatus = 'DRAFT' | 'PROPOSED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'SUPERSEDED';
export type AdrStatusFilter = AdrStatus | 'ALL';
export type AdrTabKey = 'context' | 'decision' | 'consequences' | 'alternatives';

export interface AdrDto {
  id: string;
  adrNumber: number;
  title: string;
  status: AdrStatus;
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
}

export type Adr = AdrDto;

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
  me: { id: string; role: AdrStatus | string }
): AdrStatus[] {
  const role = me.role as string;
  const isOwner = adr.authorId === me.id;
  const isAdmin = role === 'ADMIN';

  if (isAdmin) {
    return ['DRAFT', 'PROPOSED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'SUPERSEDED']
      .filter((status) => status !== adr.status) as AdrStatus[];
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
      return ['SUPERSEDED'];
    case 'REJECTED':
      return role === 'AUTHOR' && isOwner ? ['DRAFT'] : [];
    default:
      return [];
  }
}
