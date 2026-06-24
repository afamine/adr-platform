import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TotpService } from '../../services/totp.service';
import { TotpSetupResponse, TotpEnableRequest } from '../../models/auth.models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-enable-2fa-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './enable-2fa-modal.component.html',
  styleUrl: './enable-2fa-modal.component.scss'
})
export class Enable2faModalComponent implements OnChanges, OnDestroy {
  private readonly totp = inject(TotpService);

  @Input() visible = false;
  @Output() closed = new EventEmitter<void>();
  @Output() enabled = new EventEmitter<void>();

  step = signal<1 | 2 | 'success'>(1);
  isLoading = signal(false);
  errorMsg = signal<string | null>(null);
  copied = signal(false);

  setupData: TotpSetupResponse | null = null;

  codeControl = new FormControl('', [Validators.required, Validators.pattern('[0-9]{6}')]);

  private sub: Subscription = Subscription.EMPTY;
  private successTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible) {
      // when opened, reset to step 1 and load QR
      this.resetState();
      if (this.step() === 1) {
        this.loadSetupData();
      }
    }
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
    if (this.successTimer) {
      clearTimeout(this.successTimer);
    }
  }

  private resetState(): void {
    this.step.set(1);
    this.isLoading.set(false);
    this.errorMsg.set(null);
    this.setupData = null;
    this.codeControl.reset('');
    this.copied.set(false);
  }

  loadSetupData(): void {
    this.isLoading.set(true);
    this.errorMsg.set(null);
    this.sub.unsubscribe();
    this.sub = this.totp.setup().subscribe(
      (data: TotpSetupResponse) => {
        // Format secret into groups of 4 for display
        if (data && data.secret) {
          data.secret = (data.secret || '').replace(/(.{4})/g, '$1 ').trim();
        }
        this.setupData = data;
        this.isLoading.set(false);
      },
      () => {
        this.isLoading.set(false);
        this.errorMsg.set('Unable to generate setup data. Please try again.');
      }
    );
  }

  goToStep2(): void {
    this.errorMsg.set(null);
    this.codeControl.reset('');
    this.step.set(2);
  }

  goBack(): void {
    this.errorMsg.set(null);
    this.codeControl.reset('');
    this.step.set(1);
  }

  onEnable(): void {
    if (!this.setupData) return;
    if (this.codeControl.invalid) {
      this.errorMsg.set('Invalid code. Please check your authenticator app.');
      return;
    }

    const rawCode = String(this.codeControl.value || '').trim();

    this.isLoading.set(true);
    this.errorMsg.set(null);

    const payload: TotpEnableRequest = { code: rawCode };
    this.sub.unsubscribe();
    this.sub = this.totp.enable(payload).subscribe(
      (_resp: { message: string }) => {
        this.isLoading.set(false);
        this.step.set('success');
        this.enabled.emit();
        // auto-close after 2s
        this.successTimer = setTimeout(() => this.onDone(), 2000);
      },
      () => {
        this.isLoading.set(false);
        this.errorMsg.set('Invalid code. Please check your authenticator app.');
      }
    );
  }

  onDone(): void {
    this.closed.emit();
    this.resetState();
  }

  onCancel(): void {
    this.closed.emit();
    this.resetState();
  }

  async copySecret(): Promise<void> {
    if (!this.setupData) return;
    try {
      await navigator.clipboard.writeText(this.setupData.secret.replace(/\s/g, ''));
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 1200);
    } catch {
      // ignore
    }
  }
}

