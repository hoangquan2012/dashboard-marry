import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, effect, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Renders an image that lives behind the JWT-protected API (e.g. QR codes):
 * the file is fetched with the Authorization header and shown through an object URL.
 */
@Component({
  selector: 'app-secure-image',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    @if (url()) {
      <img [src]="url()" [alt]="alt()" [style.width.px]="size()" [style.height.px]="size()" />
    } @else if (failed()) {
      <div class="fallback" [style.width.px]="size()" [style.height.px]="size()">Không tải được ảnh</div>
    } @else {
      <div class="fallback" [style.width.px]="size()" [style.height.px]="size()"><mat-spinner [diameter]="24"></mat-spinner></div>
    }
  `,
  styles: [
    `
      :host { display: inline-block; }
      img { display: block; border-radius: 8px; background: #fff; }
      .fallback { display: grid; place-items: center; background: var(--wm-cream); border-radius: 8px; font-size: 12px; color: var(--wm-muted); }
    `
  ]
})
export class SecureImageComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);

  readonly src = input.required<string>();
  readonly alt = input('');
  readonly size = input(200);

  readonly url = signal<SafeUrl | null>(null);
  readonly failed = signal(false);
  private objectUrl: string | null = null;

  constructor() {
    effect(() => {
      const src = this.src();
      this.revoke();
      this.url.set(null);
      this.failed.set(false);
      this.http.get(src, { responseType: 'blob' }).subscribe({
        next: (blob) => {
          this.objectUrl = URL.createObjectURL(blob);
          this.url.set(this.sanitizer.bypassSecurityTrustUrl(this.objectUrl));
        },
        error: () => this.failed.set(true)
      });
    });
  }

  ngOnDestroy(): void {
    this.revoke();
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
