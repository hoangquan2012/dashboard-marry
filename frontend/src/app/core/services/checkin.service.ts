import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckinResult, CheckinStats, Invitation } from '../models/models';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class CheckinService {
  private readonly api = inject(ApiService);

  stats(eventId: number): Observable<CheckinStats> {
    return this.api.get<CheckinStats>('/checkin/stats', { eventId });
  }

  search(eventId: number, q?: string, checkedIn?: boolean | null): Observable<Invitation[]> {
    return this.api.get<Invitation[]>('/checkin/search', { eventId, q, checkedIn });
  }

  checkinByQr(code: string, eventId: number | null, actualCount?: number | null, force = false): Observable<CheckinResult> {
    return this.api.post<CheckinResult>('/checkin/qr', { code, eventId, actualCount, force });
  }

  checkin(invitationId: number, actualCount?: number | null, force = false): Observable<CheckinResult> {
    return this.api.post<CheckinResult>(`/checkin/${invitationId}`, { actualCount, force });
  }

  undo(invitationId: number): Observable<Invitation> {
    return this.api.post<Invitation>(`/checkin/${invitationId}/undo`);
  }
}
