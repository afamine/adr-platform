import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

marked.setOptions({ gfm: true, breaks: true });

@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value?.trim()) {
      return '' as SafeHtml;
    }

    const rawHtml = marked.parse(value) as string;
    return (this.sanitizer.sanitize(SecurityContext.HTML, rawHtml) ?? '') as SafeHtml;
  }
}
