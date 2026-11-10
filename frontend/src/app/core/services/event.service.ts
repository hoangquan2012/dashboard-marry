import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EventRequest, Invitation, WeddingEvent } from '../models/models';
import { InvitationStatus, Side } from '../models/enums';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly api = inject(ApiService);

  list(): Observable<WeddingEvent[]> {
    return this.api.get<WeddingEvent[]>('/events');
  }

  get(id: number): Observable<WeddingEvent> {
    return this.api.get<WeddingEvent>(`/events/${id}`);
  }

  create(request: EventRequest): Observable<WeddingEvent> {
    return this.api.post<WeddingEvent>('/events', request);
  }

  update(id: number, request: EventRequest): Observable<WeddingEvent> {
    return this.api.put<WeddingEvent>(`/events/${id}`, request);
  }

  setMain(id: number): Observable<WeddingEvent> {
    return this.api.post<WeddingEvent>(`/events/${id}/set-main`);
  }

  delete(id: number): Observable<void> {
    return this.api.delete(`/events/${id}`);
  }

  guests(id: number, status?: InvitationStatus | null, side?: Side | null, q?: string): Observable<Invitation[]> {
    return this.api.get<Invitation[]>(`/events/${id}/guests`, { status, side, q });
  }

  invite(id: number, guestIds: number[], invitedCount = 1): Observable<{ invited: number }> {
    return this.api.post<{ invited: number }>(`/events/${id}/guests`, { guestIds, invitedCount });
  }

  removeGuest(id: number, guestId: number): Observable<void> {
    return this.api.delete(`/events/${id}/guests/${guestId}`);
  }
}
