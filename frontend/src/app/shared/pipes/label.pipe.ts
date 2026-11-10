import { Pipe, PipeTransform } from '@angular/core';
import {
  CONTRACT_STATUS_LABELS,
  EVENT_STATUS_LABELS,
  GENDER_LABELS,
  GIFT_TYPE_LABELS,
  GUEST_CATEGORY_LABELS,
  INVITATION_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  PAYMENT_TYPE_LABELS,
  SIDE_LABELS,
  TASK_PRIORITY_LABELS,
  TASK_STATUS_LABELS,
  USER_ROLE_LABELS
} from '../../core/models/enums';

type LabelKind =
  | 'side'
  | 'gender'
  | 'category'
  | 'invitationStatus'
  | 'giftType'
  | 'paymentStatus'
  | 'paymentType'
  | 'paymentMethod'
  | 'taskStatus'
  | 'taskPriority'
  | 'eventStatus'
  | 'contractStatus'
  | 'role';

const MAPS: Record<LabelKind, Record<string, string>> = {
  side: SIDE_LABELS,
  gender: GENDER_LABELS,
  category: GUEST_CATEGORY_LABELS,
  invitationStatus: INVITATION_STATUS_LABELS,
  giftType: GIFT_TYPE_LABELS,
  paymentStatus: PAYMENT_STATUS_LABELS,
  paymentType: PAYMENT_TYPE_LABELS,
  paymentMethod: PAYMENT_METHOD_LABELS,
  taskStatus: TASK_STATUS_LABELS,
  taskPriority: TASK_PRIORITY_LABELS,
  eventStatus: EVENT_STATUS_LABELS,
  contractStatus: CONTRACT_STATUS_LABELS,
  role: USER_ROLE_LABELS
};

/** Translates enum values into Vietnamese labels: {{ guest.side | label:'side' }} */
@Pipe({ name: 'label', standalone: true })
export class LabelPipe implements PipeTransform {
  transform(value: string | null | undefined, kind: LabelKind): string {
    if (!value) return '—';
    return MAPS[kind][value] ?? value;
  }
}
