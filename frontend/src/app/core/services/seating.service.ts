import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SeatLocation, SeatingOverview, TableArea, TableRequest, UnassignedGuest, WeddingTable } from '../models/models';
import { Side } from '../models/enums';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class SeatingService {
  private readonly api = inject(ApiService);

  overview(eventId: number, side?: Side | null, q?: string): Observable<SeatingOverview> {
    return this.api.get<SeatingOverview>('/seating/overview', { eventId, side, q });
  }

  unassigned(eventId: number, side?: Side | null, q?: string): Observable<UnassignedGuest[]> {
    return this.api.get<UnassignedGuest[]>('/seating/unassigned', { eventId, side, q });
  }

  find(eventId: number, q: string): Observable<SeatLocation[]> {
    return this.api.get<SeatLocation[]>('/seating/find', { eventId, q });
  }

  createArea(eventId: number, name: string, description: string | null): Observable<TableArea> {
    return this.api.post<TableArea>('/seating/areas', { eventId, name, description });
  }

  updateArea(id: number, eventId: number, name: string, description: string | null): Observable<TableArea> {
    return this.api.put<TableArea>(`/seating/areas/${id}`, { eventId, name, description });
  }

  deleteArea(id: number): Observable<void> {
    return this.api.delete(`/seating/areas/${id}`);
  }

  createTable(request: TableRequest): Observable<WeddingTable> {
    return this.api.post<WeddingTable>('/seating/tables', request);
  }

  bulkCreate(eventId: number, areaId: number | null, prefix: string, count: number, capacity: number, side: Side): Observable<WeddingTable[]> {
    return this.api.post<WeddingTable[]>('/seating/tables/bulk', { eventId, areaId, prefix, count, capacity, side });
  }

  updateTable(id: number, request: TableRequest): Observable<WeddingTable> {
    return this.api.put<WeddingTable>(`/seating/tables/${id}`, request);
  }

  deleteTable(id: number): Observable<void> {
    return this.api.delete(`/seating/tables/${id}`);
  }

  assign(tableId: number, guestId: number, seats: number | null, force = false, note: string | null = null): Observable<WeddingTable> {
    return this.api.post<WeddingTable>(`/seating/tables/${tableId}/assign`, { guestId, seats, force, note });
  }

  move(assignmentId: number, tableId: number, force = false): Observable<WeddingTable> {
    return this.api.post<WeddingTable>(`/seating/assignments/${assignmentId}/move`, { tableId, force });
  }

  updateSeats(assignmentId: number, seats: number, force = false): Observable<WeddingTable> {
    return this.api.patch<WeddingTable>(`/seating/assignments/${assignmentId}/seats`, {}, { seats, force });
  }

  unassign(assignmentId: number): Observable<void> {
    return this.api.delete(`/seating/assignments/${assignmentId}`);
  }
}
