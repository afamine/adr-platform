import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-security-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './security-card.component.html',
  styleUrl: './security-card.component.scss'
})
export class SecurityCardComponent {
  @Input() totpEnabled = false;
  @Input() isLoading = false;

  @Output() enableClicked = new EventEmitter<void>();
  @Output() disableClicked = new EventEmitter<void>();
  @Output() changePasswordClicked = new EventEmitter<void>();
  @Output() manageSessionsClicked = new EventEmitter<void>();

  get totpStatusLabel(): string {
    return this.totpEnabled ? 'Enabled' : 'Disabled';
  }

  get totpActionLabel(): string {
    return this.totpEnabled ? 'Disable 2FA' : 'Enable 2FA';
  }

  onEnable(): void {
    if (this.isLoading) return;
    this.enableClicked.emit();
  }

  onDisable(): void {
    if (this.isLoading) return;
    this.disableClicked.emit();
  }

  onChangePassword(): void {
    this.changePasswordClicked.emit();
  }

  onManageSessions(): void {
    this.manageSessionsClicked.emit();
  }
}

