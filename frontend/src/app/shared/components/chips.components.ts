import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import {
  INVITATION_STATUS_CLASS,
  INVITATION_STATUS_LABELS,
  InvitationStatus,
  PAYMENT_STATUS_CLASS,
  PAYMENT_STATUS_LABELS,
  PaymentStatus,
  SIDE_LABELS,
  Side,
  sideClass
} from '../../core/models/enums';

@Component({
  selector: 'app-side-chip',
  standalone: true,
  template: `<span class="chip" [class]="'chip ' + cls()">{{ label() }}</span>`
})
export class SideChipComponent {
  readonly side = input.required<Side | null | undefined>();
  readonly cls = computed(() => sideClass(this.side()));
  readonly label = computed(() => (this.side() ? SIDE_LABELS[this.side()!] : 'Chung'));
}

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <span [class]="'chip ' + cls()">
      @if (icon()) {
        <mat-icon class="material-symbols-rounded">{{ icon() }}</mat-icon>
      }
      {{ label() }}
    </span>
  `
})
export class StatusChipComponent {
  readonly status = input.required<InvitationStatus | null | undefined>();
  readonly cls = computed(() => (this.status() ? INVITATION_STATUS_CLASS[this.status()!] : 'chip-neutral'));
  readonly label = computed(() => (this.status() ? INVITATION_STATUS_LABELS[this.status()!] : 'Chưa mời'));
  readonly icon = computed(() => {
    switch (this.status()) {
      case 'CONFIRMED':
        return 'check_circle';
      case 'ATTENDED':
        return 'how_to_reg';
      case 'DECLINED':
        return 'cancel';
      case 'PENDING':
        return 'schedule';
      case 'SENT':
        return 'send';
      case 'ABSENT':
        return 'person_off';
      default:
        return 'mail';
    }
  });
}

@Component({
  selector: 'app-payment-chip',
  standalone: true,
  template: `<span [class]="'chip ' + cls()">{{ label() }}</span>`
})
export class PaymentChipComponent {
  readonly status = input.required<PaymentStatus>();
  readonly cls = computed(() => PAYMENT_STATUS_CLASS[this.status()]);
  readonly label = computed(() => PAYMENT_STATUS_LABELS[this.status()]);
}
