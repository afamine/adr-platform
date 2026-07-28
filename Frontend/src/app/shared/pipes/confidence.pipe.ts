import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'confidence',
  standalone: true
})
export class ConfidencePipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || !Number.isFinite(value)) {
      return 'N/A';
    }

    const percentage = Math.round(Math.min(Math.max(value, 0), 1) * 100);
    return `${percentage}%`;
  }
}
