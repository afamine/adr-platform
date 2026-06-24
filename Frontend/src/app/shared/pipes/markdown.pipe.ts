import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

// Configure marked once at module load
marked.setOptions({ gfm: true, breaks: true });

@Pipe({
  name: 'markdown',
  standalone: true,
})
export class MarkdownPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value?.trim()) return '';
    const rawHtml = marked.parse(value) as string;
    // Sanitize before rendering — prevents XSS
    return this.sanitizer.sanitize(SecurityContext.HTML, rawHtml) ?? '';
  }
}
