export type Side = 'GROOM' | 'BRIDE' | 'BOTH';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type GuestCategory = 'FAMILY' | 'RELATIVE' | 'FRIEND' | 'COLLEAGUE' | 'NEIGHBOR' | 'PARENTS_GUEST' | 'OTHER';
export type InvitationStatus = 'NOT_SENT' | 'SENT' | 'PENDING' | 'CONFIRMED' | 'DECLINED' | 'ATTENDED' | 'ABSENT';
export type GiftType = 'CASH' | 'BANK_TRANSFER' | 'GOLD' | 'IN_KIND' | 'OTHER';
export type PaymentStatus = 'UNPAID' | 'DEPOSITED' | 'PARTIAL' | 'PAID';
export type PaymentType = 'DEPOSIT' | 'PARTIAL' | 'FINAL';
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'OTHER';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type EventStatus = 'PLANNED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
export type ContractStatus = 'NEGOTIATING' | 'CONTRACTED' | 'DEPOSITED' | 'COMPLETED' | 'CANCELLED';
export type UserRole = 'ADMIN' | 'STAFF';
export type AttachmentEntityType = 'EXPENSE' | 'VENDOR' | 'GIFT' | 'EVENT' | 'TASK' | 'OTHER';

export interface Option<T extends string> {
  value: T;
  label: string;
}

export const SIDE_LABELS: Record<Side, string> = {
  GROOM: 'Nhà trai',
  BRIDE: 'Nhà gái',
  BOTH: 'Chung'
};

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác'
};

export const GUEST_CATEGORY_LABELS: Record<GuestCategory, string> = {
  FAMILY: 'Gia đình',
  RELATIVE: 'Họ hàng',
  FRIEND: 'Bạn bè',
  COLLEAGUE: 'Đồng nghiệp',
  NEIGHBOR: 'Hàng xóm',
  PARENTS_GUEST: 'Khách của bố mẹ',
  OTHER: 'Khác'
};

export const INVITATION_STATUS_LABELS: Record<InvitationStatus, string> = {
  NOT_SENT: 'Chưa gửi lời mời',
  SENT: 'Đã gửi lời mời',
  PENDING: 'Chưa xác nhận',
  CONFIRMED: 'Đã xác nhận tham dự',
  DECLINED: 'Từ chối tham dự',
  ATTENDED: 'Đã tham dự',
  ABSENT: 'Vắng mặt'
};

export const INVITATION_STATUS_CLASS: Record<InvitationStatus, string> = {
  NOT_SENT: 'chip-neutral',
  SENT: 'chip-info',
  PENDING: 'chip-warning',
  CONFIRMED: 'chip-success',
  DECLINED: 'chip-danger',
  ATTENDED: 'chip-primary',
  ABSENT: 'chip-neutral'
};

export const GIFT_TYPE_LABELS: Record<GiftType, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  GOLD: 'Vàng',
  IN_KIND: 'Hiện vật',
  OTHER: 'Khác'
};

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  UNPAID: 'Chưa thanh toán',
  DEPOSITED: 'Đã đặt cọc',
  PARTIAL: 'Thanh toán một phần',
  PAID: 'Đã thanh toán'
};

export const PAYMENT_STATUS_CLASS: Record<PaymentStatus, string> = {
  UNPAID: 'chip-danger',
  DEPOSITED: 'chip-warning',
  PARTIAL: 'chip-info',
  PAID: 'chip-success'
};

export const PAYMENT_TYPE_LABELS: Record<PaymentType, string> = {
  DEPOSIT: 'Đặt cọc',
  PARTIAL: 'Thanh toán một phần',
  FINAL: 'Thanh toán nốt'
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  OTHER: 'Khác'
};

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'Chưa thực hiện',
  IN_PROGRESS: 'Đang thực hiện',
  DONE: 'Hoàn thành'
};

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp'
};

export const TASK_PRIORITY_CLASS: Record<TaskPriority, string> = {
  LOW: 'chip-neutral',
  MEDIUM: 'chip-info',
  HIGH: 'chip-warning',
  URGENT: 'chip-danger'
};

export const EVENT_STATUS_LABELS: Record<EventStatus, string> = {
  PLANNED: 'Dự kiến',
  CONFIRMED: 'Đã chốt',
  COMPLETED: 'Đã diễn ra',
  CANCELLED: 'Đã hủy'
};

export const EVENT_STATUS_CLASS: Record<EventStatus, string> = {
  PLANNED: 'chip-info',
  CONFIRMED: 'chip-success',
  COMPLETED: 'chip-neutral',
  CANCELLED: 'chip-danger'
};

export const CONTRACT_STATUS_LABELS: Record<ContractStatus, string> = {
  NEGOTIATING: 'Đang thương lượng',
  CONTRACTED: 'Đã ký hợp đồng',
  DEPOSITED: 'Đã đặt cọc',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy'
};

export const CONTRACT_STATUS_CLASS: Record<ContractStatus, string> = {
  NEGOTIATING: 'chip-warning',
  CONTRACTED: 'chip-info',
  DEPOSITED: 'chip-primary',
  COMPLETED: 'chip-success',
  CANCELLED: 'chip-danger'
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Quản trị viên',
  STAFF: 'Nhân viên hỗ trợ'
};

export function toOptions<T extends string>(labels: Record<T, string>): Option<T>[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
}

export const SIDE_OPTIONS = toOptions(SIDE_LABELS);
export const GENDER_OPTIONS = toOptions(GENDER_LABELS);
export const GUEST_CATEGORY_OPTIONS = toOptions(GUEST_CATEGORY_LABELS);
export const INVITATION_STATUS_OPTIONS = toOptions(INVITATION_STATUS_LABELS);
export const GIFT_TYPE_OPTIONS = toOptions(GIFT_TYPE_LABELS);
export const PAYMENT_STATUS_OPTIONS = toOptions(PAYMENT_STATUS_LABELS);
export const PAYMENT_TYPE_OPTIONS = toOptions(PAYMENT_TYPE_LABELS);
export const PAYMENT_METHOD_OPTIONS = toOptions(PAYMENT_METHOD_LABELS);
export const TASK_STATUS_OPTIONS = toOptions(TASK_STATUS_LABELS);
export const TASK_PRIORITY_OPTIONS = toOptions(TASK_PRIORITY_LABELS);
export const EVENT_STATUS_OPTIONS = toOptions(EVENT_STATUS_LABELS);
export const CONTRACT_STATUS_OPTIONS = toOptions(CONTRACT_STATUS_LABELS);
export const USER_ROLE_OPTIONS = toOptions(USER_ROLE_LABELS);

export function sideClass(side: Side | null | undefined): string {
  switch (side) {
    case 'GROOM':
      return 'chip-groom';
    case 'BRIDE':
      return 'chip-bride';
    default:
      return 'chip-both';
  }
}
