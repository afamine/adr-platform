import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Adr } from '../../../../models/adr.model';

type InsightImpact = 'high' | 'medium' | 'low';

interface AiInsight {
  icon: 'lightbulb' | 'warning' | 'check';
  title: string;
  confidence: number;
  impact: InsightImpact;
  description: string;
  rationale: string;
  source: string;
}

@Component({
  selector: 'app-ai-assistant-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-assistant-panel.component.html',
  styleUrl: './ai-assistant-panel.component.scss'
})
export class AiAssistantPanelComponent {
  @Input() selectedAdr: Adr | null = null;
  @Output() closePanel = new EventEmitter<void>();

  readonly aiInsights: AiInsight[] = [
    {
      icon: 'lightbulb',
      title: 'Consider Event-Driven Architecture',
      confidence: 87,
      impact: 'high',
      description:
        'Based on your microservices context, an event-driven approach could improve scalability and reduce coupling.',
      rationale:
        'Analysis of similar decisions in 247 projects shows 82% success rate with event-driven patterns for distributed systems.',
      source: 'Pattern matching: Microservices + Scalability'
    },
    {
      icon: 'warning',
      title: 'Potential Performance Bottleneck',
      confidence: 72,
      impact: 'medium',
      description: 'Synchronous communication between services may create latency issues under high load.',
      rationale: 'Projects with similar synchronous architectures report 3x more latency issues at 10k+ req/min.',
      source: 'Historical performance data'
    },
    {
      icon: 'check',
      title: 'Strong Alignment with Industry Standards',
      confidence: 94,
      impact: 'low',
      description:
        'Your decision aligns well with current microservices best practices documented in major case studies.',
      rationale: 'Netflix, Uber, and Airbnb use similar patterns with documented success.',
      source: 'Industry benchmark database'
    }
  ];

  getImpactLabel(impact: InsightImpact): string {
    if (impact === 'high') {
      return 'high impact';
    }

    if (impact === 'medium') {
      return 'medium impact';
    }

    return 'low impact';
  }

  getConfidenceBadgeClass(impact: InsightImpact): string {
    if (impact === 'high') {
      return 'ai-panel__confidence--high';
    }

    if (impact === 'medium') {
      return 'ai-panel__confidence--medium';
    }

    return 'ai-panel__confidence--low';
  }

  getInsightIcon(variant: AiInsight['icon']): string {
    if (variant === 'warning') {
      return '⚠';
    }

    if (variant === 'check') {
      return '✓';
    }

    return '💡';
  }
}
