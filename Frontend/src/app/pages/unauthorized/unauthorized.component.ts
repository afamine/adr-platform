import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="unauthorized-page">
      <h1>Access Denied</h1>
      <p>You don't have permission to view this page.</p>
      <a routerLink="/adrs">Back to ADRs</a>
    </section>
  `,
  styles: [
    `
      .unauthorized-page {
        min-height: 100vh;
        display: grid;
        place-content: center;
        text-align: center;
        gap: 0.75rem;
        padding: 1.5rem;
      }

      h1 {
        margin: 0;
        font-size: 2rem;
      }

      p {
        margin: 0;
        color: #475569;
      }

      a {
        color: #0f172a;
        font-weight: 600;
      }
    `
  ]
})
export class UnauthorizedComponent {}
