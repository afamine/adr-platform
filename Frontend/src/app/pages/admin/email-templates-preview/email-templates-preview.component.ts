import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { EmailTemplatePreviewService } from '../../../services/email-template-preview.service';

type TemplateName = 'email-verification' | 'password-reset';

@Component({
  selector: 'app-email-templates-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './email-templates-preview.component.html',
  styleUrl: './email-templates-preview.component.scss'
})
export class EmailTemplatesPreviewComponent implements OnInit {
  private readonly previews = inject(EmailTemplatePreviewService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly templates: Array<{ name: TemplateName; label: string; file: string }> = [
    { name: 'email-verification', label: 'Email verification', file: 'email-verification.html' },
    { name: 'password-reset', label: 'Password reset', file: 'password-reset-email.html' }
  ];
  selected: TemplateName = 'email-verification';
  previewHtml: SafeHtml | null = null;
  isLoading = false;
  errorMessage = '';

  ngOnInit(): void { this.loadPreview(this.selected); }

  loadPreview(templateName: TemplateName): void {
    this.selected = templateName;
    this.isLoading = true;
    this.errorMessage = '';
    this.previews.getPreview(templateName).subscribe({
      next: (html) => {
        this.previewHtml = this.sanitizer.bypassSecurityTrustHtml(html);
        this.isLoading = false;
      },
      error: (error) => {
        this.previewHtml = null;
        this.isLoading = false;
        this.errorMessage = error?.error?.message || 'Unable to render this email preview.';
      }
    });
  }

  selectedFile(): string { return this.templates.find((template) => template.name === this.selected)?.file ?? ''; }
}
