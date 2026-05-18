import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ConfirmConfig {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  danger: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly _config$ = new BehaviorSubject<ConfirmConfig | null>(null);
  readonly config$ = this._config$.asObservable();
  private resolver!: (value: boolean) => void;

  confirm(config: ConfirmConfig): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
      this._config$.next(config);
    });
  }

  resolve(result: boolean): void {
    this.resolver(result);
    this._config$.next(null);
  }
}
