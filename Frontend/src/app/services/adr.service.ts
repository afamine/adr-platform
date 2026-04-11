import { computed, Injectable, signal } from '@angular/core';
import { Adr, AdrSection, AdrStatus, AdrStatusFilter, AdrTabKey } from '../models/adr.model';

const buildSections = (content: Record<AdrTabKey, string>): Record<AdrTabKey, AdrSection> => ({
  context: {
    label: 'Context',
    question: 'What is the context and background for this decision?',
    placeholder: 'Describe the architectural context, constraints, and requirements that led to this decision...',
    content: content.context
  },
  decision: {
    label: 'Decision',
    question: 'What decision was made?',
    placeholder: 'Explain the chosen architectural direction, scope, and implementation boundaries...',
    content: content.decision
  },
  consequences: {
    label: 'Consequences',
    question: 'What are the expected outcomes and trade-offs?',
    placeholder: 'Capture the positive impact, drawbacks, risks, and operational implications of this choice...',
    content: content.consequences
  },
  alternatives: {
    label: 'Alternatives',
    question: 'What alternatives were considered?',
    placeholder: 'List the competing options and explain why they were not selected...',
    content: content.alternatives
  }
});

const MOCK_ADRS: Adr[] = [
  {
    id: 'adr-001',
    title: 'Adopt Microservices Architecture',
    summary: 'Transition from monolithic architecture to microservices for scalability and team autonomy.',
    status: 'accepted',
    updatedAt: '2026-03-28',
    tags: ['architecture', 'microservices', 'scalability'],
    author: 'Michael Brown',
    sections: buildSections({
      context: '',
      decision: 'We will split the platform into independently deployable services organised around bounded contexts such as identity, orders, and reporting.',
      consequences: 'This enables team autonomy and independent scaling, but introduces operational complexity, service contracts, and distributed tracing needs.',
      alternatives: 'We considered keeping a modular monolith with stricter boundaries, but it would not provide the scaling flexibility required for projected growth.'
    })
  },
  {
    id: 'adr-002',
    title: 'Implement CQRS Pattern for Order Management',
    summary: 'Separate read and write operations to optimize performance for high-volume order workloads.',
    status: 'proposed',
    updatedAt: '2026-03-24',
    tags: ['pattern', 'CQRS'],
    author: 'Sarah Kim',
    sections: buildSections({
      context: 'The order domain has complex write validation and read-heavy dashboard traffic, which leads to contention on the current relational model.',
      decision: 'Adopt CQRS for the order domain only, with dedicated write models and denormalized read projections for reporting use cases.',
      consequences: 'Read performance improves and write models stay focused, but eventual consistency must be communicated clearly to product teams.',
      alternatives: 'Alternative options included database indexing and view optimization, but those approaches do not address long-term write complexity.'
    })
  },
  {
    id: 'adr-003',
    title: 'Choose PostgreSQL for Primary Database',
    summary: 'Select PostgreSQL as the primary relational database for compliance, extensibility, and JSON support.',
    status: 'accepted',
    updatedAt: '2026-03-19',
    tags: ['database', 'PostgreSQL'],
    author: 'Aisha Bello',
    sections: buildSections({
      context: 'The platform requires transactional consistency, strong reporting capabilities, and selective schemaless fields for metadata.',
      decision: 'Use PostgreSQL as the primary data store across core transactional services.',
      consequences: 'The team benefits from a mature ecosystem and strong tooling, while still supporting flexible JSON structures where needed.',
      alternatives: 'MySQL and MongoDB were reviewed, but PostgreSQL offered the best balance of relational integrity and extensibility.'
    })
  },
  {
    id: 'adr-004',
    title: 'Event-Driven Communication Between Services',
    summary: 'Use Apache Kafka for asynchronous event-driven communication between microservices.',
    status: 'draft',
    updatedAt: '2026-03-16',
    tags: ['messaging', 'events'],
    author: 'Daniel Ortiz',
    sections: buildSections({
      context: 'As the number of services increases, direct synchronous dependencies risk reducing resilience and increasing latency chains.',
      decision: 'Introduce Kafka topics for domain events and use asynchronous subscribers for non-blocking downstream processing.',
      consequences: 'This improves decoupling and resilience, but requires schema governance, replay handling, and observability investments.',
      alternatives: 'RabbitMQ and direct REST callbacks were considered, but Kafka better fits throughput and replay requirements.'
    })
  },
  {
    id: 'adr-005',
    title: 'GraphQL API Gateway Implementation',
    summary: 'Implement a GraphQL gateway to provide a unified API layer for frontend clients.',
    status: 'proposed',
    updatedAt: '2026-03-11',
    tags: ['API', 'GraphQL'],
    author: 'Nina Patel',
    sections: buildSections({
      context: 'Frontend teams currently aggregate data from multiple REST endpoints, resulting in over-fetching and duplicate integration logic.',
      decision: 'Add a GraphQL gateway for frontend composition while keeping internal service contracts REST-based during the first phase.',
      consequences: 'Frontend delivery becomes faster and more flexible, but schema ownership and gateway performance must be managed carefully.',
      alternatives: 'Backend-for-frontend services were explored, but a shared GraphQL gateway reduces duplicated aggregation logic.'
    })
  },
  {
    id: 'adr-006',
    title: 'JWT-based Authentication Strategy',
    summary: 'Standardize token-based authentication for API access and service-to-service requests.',
    status: 'accepted',
    updatedAt: '2026-03-07',
    tags: ['security', 'JWT'],
    author: 'Michael Brown',
    sections: buildSections({
      context: 'The system needs stateless authentication for web clients and internal services while maintaining role-based authorization.',
      decision: 'Adopt short-lived JWT access tokens backed by refresh token rotation for user sessions and signed service credentials internally.',
      consequences: 'This simplifies horizontal scaling, but token revocation and key rotation workflows must be formalized.',
      alternatives: 'Server-side sessions were considered, but they add coordination overhead for distributed deployments.'
    })
  }
];

@Injectable({
  providedIn: 'root'
})
export class AdrService {
  private readonly adrItems = signal<Adr[]>(MOCK_ADRS);
  private readonly selectedAdrId = signal<string>(MOCK_ADRS[0]?.id ?? '');

  readonly searchQuery = signal('');
  readonly statusFilter = signal<AdrStatusFilter>('all');
  readonly activeTab = signal<AdrTabKey>('context');
  readonly previewMode = signal(false);
  readonly collaborateMode = signal(false);
  readonly aiAssistMode = signal(false);

  readonly filteredAdrs = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const filter = this.statusFilter();

    return this.adrItems().filter((adr) => {
      const matchesFilter = filter === 'all' || adr.status === filter;
      const matchesQuery =
        query.length === 0 ||
        `${adr.title} ${adr.summary} ${adr.tags.join(' ')}`.toLowerCase().includes(query);

      return matchesFilter && matchesQuery;
    });
  });

  readonly selectedAdr = computed(() => {
    return this.adrItems().find((adr) => adr.id === this.selectedAdrId()) ?? this.adrItems()[0] ?? null;
  });

  selectAdr(adrId: string): void {
    this.selectedAdrId.set(adrId);
    this.activeTab.set('context');
    this.previewMode.set(false);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setStatusFilter(filter: AdrStatusFilter): void {
    this.statusFilter.set(filter);
  }

  setActiveTab(tab: AdrTabKey): void {
    this.activeTab.set(tab);
  }

  updateTitle(title: string): void {
    this.patchSelectedAdr((adr) => ({
      ...adr,
      title
    }));
  }

  updateStatus(status: AdrStatus): void {
    this.patchSelectedAdr((adr) => ({
      ...adr,
      status
    }));
  }

  updateSectionContent(tab: AdrTabKey, content: string): void {
    this.patchSelectedAdr((adr) => ({
      ...adr,
      sections: {
        ...adr.sections,
        [tab]: {
          ...adr.sections[tab],
          content
        }
      }
    }));
  }

  createAdr(): void {
    const newAdr: Adr = {
      id: `adr-${Date.now()}`,
      title: 'New Architecture Decision',
      summary: 'Capture the motivation, decision, and expected impact for this new architecture record.',
      status: 'draft',
      updatedAt: new Date().toISOString().slice(0, 10),
      tags: ['new', 'draft'],
      author: 'Michael Brown',
      sections: buildSections({
        context: '',
        decision: '',
        consequences: '',
        alternatives: ''
      })
    };

    this.adrItems.update((items) => [newAdr, ...items]);
    this.selectedAdrId.set(newAdr.id);
    this.activeTab.set('context');
    this.previewMode.set(false);
  }

  togglePreview(): void {
    this.previewMode.update((value) => !value);
  }

  toggleCollaborate(): void {
    this.collaborateMode.update((value) => !value);
  }

  toggleAiAssist(): void {
    this.aiAssistMode.update((value) => !value);
  }

  saveAdr(): void {
    const currentDate = new Date().toISOString().slice(0, 10);

    this.patchSelectedAdr((adr) => ({
      ...adr,
      updatedAt: currentDate
    }));
  }

  private patchSelectedAdr(project: (adr: Adr) => Adr): void {
    const activeId = this.selectedAdrId();

    this.adrItems.update((items) =>
      items.map((adr) => {
        if (adr.id !== activeId) {
          return adr;
        }

        return project(adr);
      })
    );
  }
}
