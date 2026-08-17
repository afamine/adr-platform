import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { ToastContainerComponent } from './shared/toast-container/toast-container.component';
import { ConfirmDialogComponent } from './shared/confirm-dialog/confirm-dialog.component';
import { AxiomLogoComponent } from './shared/axiom-logo/axiom-logo.component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, ToastContainerComponent, ConfirmDialogComponent, AxiomLogoComponent],
  template: `
    <div class="app-splash" *ngIf="showSplash" aria-label="Loading Axiom ADR Manager">
      <app-axiom-logo state="splash" [size]="96" [showWordmark]="true" (animationComplete)="showSplash = false" />
    </div>
    <app-toast-container /><app-confirm-dialog /><router-outlet />
  `,
  styleUrls: ['./app.scss']
})
export class App {
  showSplash = true;
}
