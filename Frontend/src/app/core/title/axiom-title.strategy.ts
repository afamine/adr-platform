import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

@Injectable()
export class AxiomTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly fallbackTitle = 'Axiom ADR Manager';

  override updateTitle(snapshot: RouterStateSnapshot): void {
    this.title.setTitle(this.buildTitle(snapshot) ?? this.fallbackTitle);
  }
}
