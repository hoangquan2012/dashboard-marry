import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Gift, GiftFilter, GiftRequest, GiftStats, PageResponse } from '../models/models';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class GiftService {
  private readonly api = inject(ApiService);

  list(filter: GiftFilter): Observable<PageResponse<Gift>> {
    return this.api.get<PageResponse<Gift>>('/gifts', filter as Record<string, unknown>);
  }

  stats(filter: GiftFilter): Observable<GiftStats> {
    const { page, size, sortBy, direction, guestId, ...rest } = filter;
    return this.api.get<GiftStats>('/gifts/stats', rest as Record<string, unknown>);
  }

  get(id: number): Observable<Gift> {
    return this.api.get<Gift>(`/gifts/${id}`);
  }

  create(request: GiftRequest): Observable<Gift> {
    return this.api.post<Gift>('/gifts', request);
  }

  update(id: number, request: GiftRequest): Observable<Gift> {
    return this.api.put<Gift>(`/gifts/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/gifts/${id}`);
  }

  export(filter: GiftFilter): Observable<string> {
    const { page, size, sortBy, direction, guestId, ...rest } = filter;
    return this.api.download('/gifts/export', rest as Record<string, unknown>, 'tien-mung-cuoi.xlsx');
  }
}
