import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DuplicateCheck,
  Gift,
  Guest,
  GuestFilter,
  GuestGroup,
  GuestGroupRequest,
  GuestRequest,
  GuestStats,
  GuestSummary,
  ImportResult,
  Invitation,
  InvitationUpdateRequest,
  PageResponse
} from '../models/models';
import { InvitationStatus, Side } from '../models/enums';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class GuestService {
  private readonly api = inject(ApiService);

  list(filter: GuestFilter): Observable<PageResponse<Guest>> {
    return this.api.get<PageResponse<Guest>>('/guests', filter as Record<string, unknown>);
  }

  stats(): Observable<GuestStats> {
    return this.api.get<GuestStats>('/guests/stats');
  }

  lookup(q: string, limit = 15): Observable<GuestSummary[]> {
    return this.api.get<GuestSummary[]>('/guests/lookup', { q, limit });
  }

  checkDuplicate(phone?: string | null, fullName?: string | null, excludeId?: number | null): Observable<DuplicateCheck> {
    return this.api.get<DuplicateCheck>('/guests/check-duplicate', { phone, fullName, excludeId });
  }

  get(id: number): Observable<Guest> {
    return this.api.get<Guest>(`/guests/${id}`);
  }

  gifts(id: number): Observable<Gift[]> {
    return this.api.get<Gift[]>(`/guests/${id}/gifts`);
  }

  create(request: GuestRequest): Observable<Guest> {
    return this.api.post<Guest>('/guests', request);
  }

  update(id: number, request: GuestRequest): Observable<Guest> {
    return this.api.put<Guest>(`/guests/${id}`, request);
  }

  archive(id: number): Observable<void> {
    return this.api.delete(`/guests/${id}`);
  }

  restore(id: number): Observable<Guest> {
    return this.api.post<Guest>(`/guests/${id}/restore`);
  }

  hardDelete(id: number): Observable<void> {
    return this.api.delete(`/guests/${id}/permanent`);
  }

  export(filter: GuestFilter): Observable<string> {
    const { page, size, ...rest } = filter;
    return this.api.download('/guests/export', rest as Record<string, unknown>, 'danh-sach-khach-moi.xlsx');
  }

  downloadTemplate(): Observable<string> {
    return this.api.download('/guests/import-template', undefined, 'mau-nhap-khach-moi.xlsx');
  }

  import(file: File, skipDuplicates: boolean): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    return this.api.upload<ImportResult>('/guests/import', form, { skipDuplicates });
  }

  // ---- groups ----
  groups(side?: Side | null, q?: string): Observable<GuestGroup[]> {
    return this.api.get<GuestGroup[]>('/guest-groups', { side, q });
  }

  createGroup(request: GuestGroupRequest): Observable<GuestGroup> {
    return this.api.post<GuestGroup>('/guest-groups', request);
  }

  updateGroup(id: number, request: GuestGroupRequest): Observable<GuestGroup> {
    return this.api.put<GuestGroup>(`/guest-groups/${id}`, request);
  }

  deleteGroup(id: number): Observable<void> {
    return this.api.delete(`/guest-groups/${id}`);
  }

  // ---- invitations ----
  updateInvitation(id: number, request: InvitationUpdateRequest): Observable<Invitation> {
    return this.api.put<Invitation>(`/invitations/${id}`, request);
  }

  bulkStatus(invitationIds: number[], status: InvitationStatus): Observable<{ updated: number }> {
    return this.api.post<{ updated: number }>('/invitations/bulk-status', { invitationIds, status });
  }

  qrUrl(invitationId: number, size = 320): string {
    return `${this.api.baseUrl}/invitations/${invitationId}/qr.png?size=${size}`;
  }

  qrBlob(invitationId: number, size = 320): Observable<Blob> {
    return this.api.blob(`/invitations/${invitationId}/qr.png`, { size });
  }
}
