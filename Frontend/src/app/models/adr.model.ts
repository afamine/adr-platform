export type AdrStatus = 'accepted' | 'proposed' | 'draft' | 'rejected';
export type AdrStatusFilter = AdrStatus | 'all';
export type AdrTabKey = 'context' | 'decision' | 'consequences' | 'alternatives';

export interface AdrSection {
  label: string;
  question: string;
  placeholder: string;
  content: string;
}

export interface Adr {
  id: string;
  title: string;
  summary: string;
  status: AdrStatus;
  updatedAt: string;
  tags: string[];
  author: string;
  sections: Record<AdrTabKey, AdrSection>;
}

export const ADR_STATUS_OPTIONS: ReadonlyArray<{ value: AdrStatus; label: string }> = [
  { value: 'accepted', label: 'Accepted' },
  { value: 'proposed', label: 'Proposed' },
  { value: 'draft', label: 'Draft' },
  { value: 'rejected', label: 'Rejected' }
];

export const ADR_TAB_ORDER: ReadonlyArray<{ key: AdrTabKey; label: string }> = [
  { key: 'context', label: 'Context' },
  { key: 'decision', label: 'Decision' },
  { key: 'consequences', label: 'Consequences' },
  { key: 'alternatives', label: 'Alternatives' }
];
