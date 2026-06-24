import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TotpService } from '../../services/totp.service';
import { TotpDisableRequest } from '../../models/auth.models';

@Component({
  selector: 'app-disable-2fa-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './disable-2fa-modal.component.html',
  styleUrl: './disable-2fa-modal.component.scss'
})
export class Disable2faModalComponent implements OnChanges {
  private readonly totp = inject(TotpService);

  @Input() visible = false;
  @Output() closed = new EventEmitter<void>();
  @Output() disabled2fa = new EventEmitter<void>();

  isLoading = signal(false);
  errorMsg = signal<string | null>(null);

  codeControl = new FormControl('', [Validators.required, Validators.pattern('[0-9]{6}')]);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      this.codeControl.reset('');
      this.errorMsg.set(null);
    }
  }

  onDisable(): void {
    if (this.codeControl.invalid) {
      this.codeControl.markAllAsTouched();
      return;
    }

    const code = String(this.codeControl.value || '').trim();
    this.isLoading.set(true);
    this.errorMsg.set(null);

    const payload: TotpDisableRequest = { code };
    this.totp.disable(payload).subscribe(
      () => {
        this.isLoading.set(false);
        this.disabled2fa.emit();
        this.closed.emit();
        this.reset();
      },
      () => {
        this.isLoading.set(false);
        this.errorMsg.set('Invalid code. Please check your authenticator app.');
      }
    );
  }

  onCancel(): void {
    this.closed.emit();
    this.reset();
  }

  private reset(): void {
    this.codeControl.reset('');
    this.errorMsg.set(null);
  }
}

