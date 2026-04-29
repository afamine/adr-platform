/**
 * AxiomLogoComponent — Angular standalone animated logo
 * 5 stages: splash | loading | success | error | idle
 * Pure CSS keyframes, no animation library required.
 *
 * NOTE: Tailwind utility classes were replaced with project-local CSS,
 * because this codebase does not use Tailwind.
 */

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';

export type LogoStage = 'splash' | 'loading' | 'success' | 'error' | 'idle';

@Component({
  selector: 'app-axiom-logo',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host { display: inline-flex; flex-direction: column; align-items: center; }

    /* Layout helpers (replacing Tailwind utilities) */
    .al-col-center { display: flex; flex-direction: column; align-items: center; gap: 1.25rem; }
    .al-row-center { display: flex; align-items: center; gap: 0.75rem; }

    /* ── Stage 1 — Splash ────────────────────────────── */
    .splash-frame    { stroke-dasharray: 1; stroke-dashoffset: 1; animation: drawPath 0.6s ease-in-out 0s forwards; }
    .splash-branch-l { stroke-dasharray: 1; stroke-dashoffset: 1; animation: drawPath 0.4s cubic-bezier(.4,0,.2,1) 0.6s forwards; }
    .splash-branch-r { stroke-dasharray: 1; stroke-dashoffset: 1; animation: drawPath 0.4s cubic-bezier(.4,0,.2,1) 0.6s forwards; }
    .splash-stem     { stroke-dasharray: 1; stroke-dashoffset: 1; animation: drawPath 0.2s cubic-bezier(.4,0,.2,1) 1.0s forwards; }
    .splash-node-top { transform: scale(0); transform-origin: 60px 15px; animation: popIn 0.15s ease-out 1.20s forwards; }
    .splash-node-l   { transform: scale(0); transform-origin: 28px 90px; animation: popIn 0.15s ease-out 1.26s forwards; }
    .splash-node-r   { transform: scale(0); transform-origin: 92px 90px; animation: popIn 0.15s ease-out 1.32s forwards; }
    .splash-char     { opacity: 0; animation: fadeIn 0.3s ease-out forwards; }

    @keyframes drawPath { to { stroke-dashoffset: 0; } }
    @keyframes popIn    { to { transform: scale(1); } }
    @keyframes fadeIn   { to { opacity: 1; } }

    /* ── Stage 2 — Loading ───────────────────────────── */
    .loading-branch   { animation: breatheLine 1.2s ease-in-out 0.2s infinite; }
    .loading-node-top { transform-origin: 60px 15px; animation: pulseNode 1.2s ease-in-out infinite; }
    .loading-node-l   { transform-origin: 28px 90px; animation: pulseNode 1.2s ease-in-out 0.4s infinite; }
    .loading-node-r   { transform-origin: 92px 90px; animation: pulseNode 1.2s ease-in-out 0.4s infinite; }

    @keyframes breatheLine { 0%,100% { opacity: 1; } 50% { opacity: 0.35; } }
    @keyframes pulseNode   { 0%,100% { transform: scale(1); } 50% { transform: scale(1.15); } }

    /* ── Stage 3 — Success ───────────────────────────── */
    .success-icon { transform-origin: center; animation: bounceIn 0.38s ease-out forwards; }
    .success-ring { transform-origin: 60px 52px; animation: ringExpand 0.4s ease-out 0.2s forwards; opacity: 0.6; }

    @keyframes bounceIn {
      0%    { transform: scale(1); }
      21%   { transform: scale(0.88); }
      68.4% { transform: scale(1.06); }
      100%  { transform: scale(1); }
    }
    @keyframes ringExpand {
      from { transform: scale(0); opacity: 0.6; }
      to   { transform: scale(2.2); opacity: 0; }
    }

    /* ── Stage 4 — Error ─────────────────────────────── */
    .error-shake { animation: shake 0.44s linear forwards; }

    @keyframes shake {
      0%    { transform: translateX(0); }
      18.2% { transform: translateX(5px); }
      40.9% { transform: translateX(-5px); }
      59.1% { transform: translateX(3px); }
      77.3% { transform: translateX(-3px); }
      90.9% { transform: translateX(1px); }
      100%  { transform: translateX(0); }
    }

    /* ── Stage 5 — Idle ──────────────────────────────── */
    .idle-float    { animation: float 3s ease-in-out infinite; }
    .idle-triangle { animation: trianglePulse 3s ease-in-out infinite; }

    @keyframes float          { 0%,100% { transform: translateY(0);   opacity: 1; } 50% { transform: translateY(-6px); opacity: 0.6; } }
    @keyframes trianglePulse  { 0%,100% { opacity: 0.15; } 50% { opacity: 0.08; } }

    /* ── Wordmark ─────────────────────────────────────── */
    .wordmark      { letter-spacing: 0.22em; font-weight: 700; display: flex; user-select: none; }
    .wordmark-wide { letter-spacing: 0.25em; }
    .wordmark-inline { letter-spacing: 0.1em; user-select: none; font-weight: 700; }

    /* ── Replay button ───────────────────────────────── */
    .replay-btn {
      margin-top: 4px;
      border-radius: 9999px;
      padding: 2px 12px;
      font-size: 11px;
      opacity: 0.4;
      border: 1px solid currentColor;
      background: transparent;
      cursor: pointer;
      transition: opacity 0.2s;
    }
    .replay-btn:hover { opacity: 0.8; }
  `],
  template: `
    <!-- ── Stage 1: Splash ─────────────────────────────── -->
    <ng-container *ngIf="stage === 'splash' && visible">
      <div class="al-col-center">
        <svg [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none">
          <path class="splash-frame"
            d="M60,4 L18,102 L102,102 Z"
            [attr.stroke]="iconColor" stroke-width="1.5" fill="none"
            pathLength="1" />
          <path class="splash-branch-l"
            d="M60,50 L28,83"
            [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" pathLength="1" />
          <path class="splash-branch-r"
            d="M60,50 L92,83"
            [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" pathLength="1" />
          <path class="splash-stem"
            d="M60,23 L60,50"
            [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" pathLength="1" />
          <g class="splash-node-top">
            <circle cx="60" cy="15" r="8" [attr.fill]="iconColor" />
          </g>
          <g class="splash-node-l">
            <circle cx="28" cy="90" r="7" [attr.fill]="iconColor" />
          </g>
          <g class="splash-node-r" (animationend)="onSplashIconEnd()">
            <circle cx="92" cy="90" r="7" [attr.fill]="iconColor" />
          </g>
        </svg>
        <div *ngIf="showWordmark" class="wordmark" [style.color]="iconColor">
          <span *ngFor="let ch of wordChars; let i = index"
            class="splash-char"
            [style.font-size.px]="wordSize"
            [style.animation-delay]="(1.38 + i * 0.03) + 's'"
            (animationend)="onSplashCharEnd(i)">{{ ch }}</span>
        </div>
      </div>
    </ng-container>

    <!-- ── Stage 2: Loading ────────────────────────────── -->
    <svg *ngIf="stage === 'loading'"
      [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none">
      <path d="M60,4 L18,102 L102,102 Z"
        [attr.stroke]="iconColor" stroke-width="1.5" fill="none" opacity="0.15" />
      <path class="loading-branch" d="M60,50 L28,83"
        [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
      <path class="loading-branch" d="M60,50 L92,83"
        [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
      <path class="loading-branch" d="M60,23 L60,50"
        [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
      <g class="loading-node-top">
        <circle cx="60" cy="15" r="8" [attr.fill]="iconColor" />
      </g>
      <g class="loading-node-l">
        <circle cx="28" cy="90" r="7" [attr.fill]="iconColor" />
      </g>
      <g class="loading-node-r">
        <circle cx="92" cy="90" r="7" [attr.fill]="iconColor" />
      </g>
    </svg>

    <!-- ── Stage 3: Success ────────────────────────────── -->
    <ng-container *ngIf="stage === 'success' && visible">
      <div [style.width.px]="svgW" [style.height.px]="size" style="position:relative;">
        <svg class="success-icon"
          [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none"
          style="position:absolute;top:0;left:0"
          (animationend)="onComplete()">
          <path d="M60,4 L18,102 L102,102 Z"
            [attr.stroke]="flashColor" stroke-width="1.5" fill="none" opacity="0.3" />
          <path d="M60,50 L28,83" [attr.stroke]="flashColor" stroke-width="2.5" stroke-linecap="round" />
          <path d="M60,50 L92,83" [attr.stroke]="flashColor" stroke-width="2.5" stroke-linecap="round" />
          <path d="M60,23 L60,50" [attr.stroke]="flashColor" stroke-width="2.5" stroke-linecap="round" />
          <circle cx="60" cy="15" r="8" [attr.fill]="flashColor" />
          <circle cx="28" cy="90" r="7" [attr.fill]="flashColor" />
          <circle cx="92" cy="90" r="7" [attr.fill]="flashColor" />
        </svg>
        <svg
          [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none"
          style="position:absolute;top:0;left:0;pointer-events:none">
          <circle class="success-ring" cx="60" cy="52" r="20"
            [attr.stroke]="accentColor" stroke-width="1.5" fill="none" />
        </svg>
      </div>
    </ng-container>

    <!-- ── Stage 4: Error ──────────────────────────────── -->
    <ng-container *ngIf="stage === 'error' && visible">
      <div class="error-shake al-row-center"
        (animationend)="onComplete()">
        <svg [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none">
          <path d="M60,4 L18,102 L102,102 Z"
            [attr.stroke]="iconColor" stroke-width="1.5" fill="none" opacity="0.3" />
          <path d="M60,50 L28,83" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
          <path d="M60,50 L92,83" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
          <path d="M60,23 L60,50" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
          <circle cx="60" cy="15" r="8" [attr.fill]="iconColor" />
          <circle cx="28" cy="90" r="7" [attr.fill]="iconColor" />
          <circle cx="92" cy="90" r="7" [attr.fill]="iconColor" />
        </svg>
        <span *ngIf="showWordmark"
          class="wordmark-inline"
          [style.color]="iconColor"
          [style.font-size.px]="fontSize">Axiom</span>
      </div>
    </ng-container>

    <!-- ── Stage 5: Idle ───────────────────────────────── -->
    <div *ngIf="stage === 'idle'" class="idle-float al-col-center">
      <svg [attr.width]="svgW" [attr.height]="size" viewBox="0 0 120 110" fill="none">
        <path class="idle-triangle"
          d="M60,4 L18,102 L102,102 Z"
          [attr.stroke]="iconColor" stroke-width="1.5" fill="none" />
        <path d="M60,50 L28,83" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
        <path d="M60,50 L92,83" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
        <path d="M60,23 L60,50" [attr.stroke]="iconColor" stroke-width="2.5" stroke-linecap="round" />
        <circle cx="60" cy="15" r="8" [attr.fill]="iconColor" />
        <circle cx="28" cy="90" r="7" [attr.fill]="iconColor" />
        <circle cx="92" cy="90" r="7" [attr.fill]="iconColor" />
      </svg>
      <span *ngIf="showWordmark" class="wordmark wordmark-wide"
        [style.color]="iconColor" [style.font-size.px]="wordSize">Axiom</span>
    </div>
  `,
})
export class AxiomLogoComponent implements OnInit, OnDestroy, OnChanges {
  @Input() stage: LogoStage = 'splash';
  @Input() iconColor = '#6366F1';
  @Input() accentColor = '#818CF8';
  @Input() size = 80;
  @Input() showWordmark = false;
  @Input() autoReplayMs?: number;

  @Output() animationComplete = new EventEmitter<void>();

  visible = true;
  flashColor = this.iconColor;
  private replayTimer?: ReturnType<typeof setInterval>;
  private flashTimers: ReturnType<typeof setTimeout>[] = [];
  private completeEmitted = false;

  get svgW(): number { return Math.round(this.size * (120 / 110)); }
  get wordSize(): number { return Math.max(10, this.size * 0.18); }
  get fontSize(): number { return Math.round(this.size * 0.38); }
  get wordChars(): string[] { return 'Axiom'.split(''); }

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.flashColor = this.iconColor;
    this.setupAutoReplay();
    if (this.stage === 'success') this.triggerFlash();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['stage'] || changes['iconColor']) {
      this.flashColor = this.iconColor;
      this.clearTimers();
      this.completeEmitted = false;
      this.setupAutoReplay();
      if (this.stage === 'success') this.triggerFlash();
    }
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  replay(): void {
    this.visible = false;
    this.completeEmitted = false;
    this.flashColor = this.iconColor;
    this.cdr.markForCheck();
    setTimeout(() => {
      this.visible = true;
      if (this.stage === 'success') this.triggerFlash();
      this.cdr.markForCheck();
    }, 30);
  }

  onSplashIconEnd(): void {
    if (!this.showWordmark) this.onComplete();
  }

  onSplashCharEnd(index: number): void {
    if (index === this.wordChars.length - 1) this.onComplete();
  }

  onComplete(): void {
    if (this.completeEmitted) return;
    this.completeEmitted = true;
    this.animationComplete.emit();
  }

  private setupAutoReplay(): void {
    if (this.replayTimer) clearInterval(this.replayTimer);
    if (!this.autoReplayMs || this.stage === 'loading' || this.stage === 'idle') return;
    this.replayTimer = setInterval(() => this.replay(), this.autoReplayMs);
  }

  private triggerFlash(): void {
    this.flashTimers.forEach(clearTimeout);
    const t1 = setTimeout(() => {
      this.flashColor = this.accentColor;
      this.cdr.markForCheck();
    }, 260);
    const t2 = setTimeout(() => {
      this.flashColor = this.iconColor;
      this.cdr.markForCheck();
    }, 340);
    this.flashTimers = [t1, t2];
  }

  private clearTimers(): void {
    if (this.replayTimer) clearInterval(this.replayTimer);
    this.flashTimers.forEach(clearTimeout);
  }
}
