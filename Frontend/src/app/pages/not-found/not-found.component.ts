import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AxiomLogoComponent } from '../../shared/axiom-logo/axiom-logo.component';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterLink, AxiomLogoComponent],
  template: `
    <section class="not-found-page">
      <app-axiom-logo state="idle" [size]="72" aria-label="Axiom" />
      <p class="not-found-page__eyebrow">404</p>
      <h1>Page not found</h1>
      <p>The page you requested is unavailable or has moved.</p>
      <a routerLink="/adrs">Back to ADRs</a>
    </section>
  `,
  styles: [`.not-found-page{min-height:100vh;display:grid;place-content:center;justify-items:center;gap:.75rem;padding:1.5rem;text-align:center}.not-found-page__eyebrow{margin:0;color:#6366f1;font-weight:700;letter-spacing:.12em}.not-found-page h1,.not-found-page p{margin:0}.not-found-page p:not(.not-found-page__eyebrow){color:#475569}.not-found-page a{color:#0f172a;font-weight:600}`]
})
export class NotFoundComponent {}
