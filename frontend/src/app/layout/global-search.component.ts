import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, filter, of, switchMap, catchError } from 'rxjs';
import { SearchHit, SearchResponse } from '../core/models/models';
import { SystemService } from '../core/services/misc.services';
import { VndPipe } from '../shared/pipes/vnd.pipe';

interface HitGroup {
  label: string;
  icon: string;
  hits: SearchHit[];
}

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatAutocompleteModule, VndPipe],
  template: `
    <mat-form-field class="search" subscriptSizing="dynamic">
      <mat-icon matPrefix class="material-symbols-rounded">search</mat-icon>
      <input
        matInput
        type="text"
        placeholder="Tìm khách, SĐT, khoản mừng, chi phí, nhà cung cấp, công việc..."
        [formControl]="control"
        [matAutocomplete]="auto" />
      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="open($event)" class="search-panel" [displayWith]="displayFn">
        @for (group of groups(); track group.label) {
          <mat-optgroup [label]="group.label">
            @for (hit of group.hits; track hit.type + hit.id) {
              <mat-option [value]="hit">
                <div class="hit">
                  <mat-icon class="material-symbols-rounded">{{ group.icon }}</mat-icon>
                  <div class="hit-body">
                    <div class="hit-title">{{ hit.title }}</div>
                    <div class="hit-sub">{{ hit.subtitle }}</div>
                  </div>
                  @if (hit.amount !== null) {
                    <span class="hit-amount">{{ hit.amount | vnd }}</span>
                  }
                </div>
              </mat-option>
            }
          </mat-optgroup>
        }
        @if (searched() && groups().length === 0) {
          <mat-option disabled>Không tìm thấy kết quả cho "{{ control.value }}"</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: [
    `
      :host { display: block; }
      .search { width: 100%; }
      .search ::ng-deep .mat-mdc-text-field-wrapper { background: #fff; border-radius: 999px; }
      .search ::ng-deep .mdc-notched-outline__leading { border-radius: 999px 0 0 999px !important; min-width: 20px !important; }
      .search ::ng-deep .mdc-notched-outline__trailing { border-radius: 0 999px 999px 0 !important; }
      .search ::ng-deep .mat-mdc-form-field-infix { min-height: 42px; padding-top: 9px !important; padding-bottom: 9px !important; }
      .hit { display: flex; align-items: center; gap: 10px; width: 100%; }
      .hit mat-icon { color: var(--wm-champagne); flex-shrink: 0; }
      .hit-body { flex: 1; min-width: 0; line-height: 1.2; }
      .hit-title { font-size: 13.5px; font-weight: 500; }
      .hit-sub { font-size: 11.5px; color: var(--wm-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
      .hit-amount { font-size: 12.5px; font-weight: 600; color: var(--wm-burgundy); white-space: nowrap; }
    `
  ]
})
export class GlobalSearchComponent {
  private readonly systemService = inject(SystemService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly control = new FormControl<string | SearchHit>('');
  readonly groups = signal<HitGroup[]>([]);
  readonly searched = signal(false);

  constructor() {
    this.control.valueChanges
      .pipe(
        filter((v): v is string => typeof v === 'string'),
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          const term = q.trim();
          if (term.length < 1) {
            this.groups.set([]);
            this.searched.set(false);
            return of(null);
          }
          return this.systemService.search(term, 6).pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res) => {
        if (!res) return;
        this.groups.set(toGroups(res));
        this.searched.set(true);
      });
  }

  displayFn = (): string => '';

  open(event: MatAutocompleteSelectedEvent): void {
    const hit = event.option.value as SearchHit;
    const [path, query] = hit.route.split('?');
    const queryParams: Record<string, string> = {};
    if (query) new URLSearchParams(query).forEach((v, k) => (queryParams[k] = v));
    this.router.navigate([path], { queryParams });
    this.control.setValue('');
    this.groups.set([]);
    this.searched.set(false);
  }
}

function toGroups(res: SearchResponse): HitGroup[] {
  const groups: HitGroup[] = [];
  if (res.guests.length) groups.push({ label: 'Khách mời', icon: 'person', hits: res.guests });
  if (res.gifts.length) groups.push({ label: 'Tiền mừng', icon: 'redeem', hits: res.gifts });
  if (res.expenses.length) groups.push({ label: 'Chi phí', icon: 'receipt_long', hits: res.expenses });
  if (res.vendors.length) groups.push({ label: 'Nhà cung cấp', icon: 'storefront', hits: res.vendors });
  if (res.tasks.length) groups.push({ label: 'Công việc', icon: 'task_alt', hits: res.tasks });
  return groups;
}
