import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { Adr, AdrStatus } from '../../../models/adr.model';
import { AdrService } from '../../../services/adr.service';
import { NotificationService } from '../../../services/notification.service';

@Injectable({ providedIn: 'root' })
export class AdrStateService {
  private readonly adrService = inject(AdrService);
  private readonly notificationService = inject(NotificationService);
  private readonly pageSize = 20;

  readonly adrs$ = signal<Adr[]>([]);
  readonly selectedAdr$ = signal<Adr | null>(null);
  readonly isLoading$ = signal<boolean>(false);
  readonly currentPage$ = signal<number>(0);
  readonly totalElements$ = signal<number>(0);
  readonly totalPages$ = signal<number>(0);
  readonly searchQuery$ = signal<string>('');
  readonly statusFilter$ = signal<AdrStatus | 'ALL'>('ALL');
  readonly tagFilter$ = signal<string>('');

  private readonly requestParams$ = computed(() => ({
    status: this.statusFilter$() !== 'ALL' ? this.statusFilter$() : undefined,
    search: this.searchQuery$().trim() || undefined,
    tag: this.tagFilter$().trim() || undefined,
    page: this.currentPage$(),
    size: this.pageSize
  }));

  constructor() {
    effect(
      () => {
        const adrs = this.adrs$();
        const selected = this.selectedAdr$();

        if (!selected) {
          return;
        }

        const normalizedSelection = adrs.find((adr) => adr.id === selected.id) ?? null;
        if (!normalizedSelection) {
          this.selectedAdr$.set(null);
          return;
        }

        if (normalizedSelection !== selected) {
          this.selectedAdr$.set(normalizedSelection);
        }
      },
      { allowSignalWrites: true }
    );
  }

  loadAdrs(selectId?: string): void {
    this.isLoading$.set(true);

    this.adrService.getAdrsPaged(this.requestParams$()).subscribe({
      next: (page) => {
        this.adrs$.set(page.content);
        this.totalElements$.set(page.totalElements);
        this.totalPages$.set(page.totalPages);
        this.isLoading$.set(false);

        const preferredId = selectId ?? this.selectedAdr$()?.id;
        const nextSelected = preferredId
          ? page.content.find((adr) => adr.id === preferredId) ?? null
          : null;

        if (nextSelected) {
          this.selectedAdr$.set(nextSelected);
        } else if (page.content.length > 0) {
          this.selectedAdr$.set(page.content[0]);
        } else {
          this.selectedAdr$.set(null);
        }
      },
      error: (err) => {
        console.error('Failed to load ADRs:', err);
        this.isLoading$.set(false);
        this.adrs$.set([]);
        this.totalElements$.set(0);
        this.totalPages$.set(0);
        this.selectedAdr$.set(null);
        this.notificationService.error('Loading failed', 'Unable to load ADRs.');
      }
    });
  }

  selectAdr(adr: Adr): void {
    this.selectedAdr$.set({ ...adr });
  }

  clearSelection(): void {
    this.selectedAdr$.set(null);
  }

  updateAdrInList(updatedAdr: Adr): void {
    this.adrs$.update((items) => items.map((adr) => (adr.id === updatedAdr.id ? updatedAdr : adr)));

    if (this.selectedAdr$()?.id === updatedAdr.id) {
      this.selectedAdr$.set(updatedAdr);
    }
  }
}
