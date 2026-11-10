import {
  AttachmentEntityType,
  ContractStatus,
  EventStatus,
  Gender,
  GiftType,
  GuestCategory,
  InvitationStatus,
  PaymentMethod,
  PaymentStatus,
  PaymentType,
  Side,
  TaskPriority,
  TaskStatus,
  UserRole
} from './enums';

// ---- common -----------------------------------------------------------------

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
  details?: Record<string, unknown>;
}

// ---- auth -------------------------------------------------------------------

export interface UserInfo {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
  lastLoginAt?: string;
  createdAt?: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

// ---- settings ---------------------------------------------------------------

export interface WeddingSettings {
  groomName: string;
  brideName: string;
  weddingDate: string | null;
  totalBudget: number;
  currency: string;
  mainVenue: string | null;
  groomFamilyInfo: string | null;
  brideFamilyInfo: string | null;
  note: string | null;
  daysUntilWedding: number | null;
  demoSeeded: boolean;
  updatedAt: string;
}

export interface WeddingSettingsRequest {
  groomName: string;
  brideName: string;
  weddingDate: string | null;
  totalBudget: number;
  mainVenue: string | null;
  groomFamilyInfo: string | null;
  brideFamilyInfo: string | null;
  note: string | null;
}

// ---- guests -----------------------------------------------------------------

export interface GuestSummary {
  id: number;
  code: string;
  fullName: string;
  phone: string | null;
  side: Side;
  groupName: string | null;
}

export interface TableInfo {
  tableId: number;
  tableName: string;
  eventId: number;
  eventName: string;
  seats: number;
}

export interface Invitation {
  id: number;
  guestId: number;
  guestCode: string;
  guestName: string;
  guestPhone: string | null;
  guestSide: Side;
  groupName: string | null;
  eventId: number;
  eventName: string;
  eventDate: string;
  status: InvitationStatus;
  statusLabel: string;
  invitedCount: number;
  expectedCount: number;
  confirmedCount: number;
  actualCount: number;
  qrToken: string;
  sentAt: string | null;
  respondedAt: string | null;
  checkedInAt: string | null;
  checkedInBy: string | null;
  note: string | null;
  tableId: number | null;
  tableName: string | null;
}

export interface Guest {
  id: number;
  code: string;
  fullName: string;
  gender: Gender | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  side: Side;
  relationship: string | null;
  category: GuestCategory;
  groupId: number | null;
  groupName: string | null;
  contactPerson: string | null;
  vip: boolean;
  note: string | null;
  overallStatus: InvitationStatus;
  totalInvited: number;
  totalExpected: number;
  totalConfirmed: number;
  totalActual: number;
  giftTotal: number;
  giftCount: number;
  invitations: Invitation[];
  tables: TableInfo[];
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InvitationInput {
  eventId: number;
  invitedCount: number;
  expectedCount: number;
  status?: InvitationStatus | null;
  note?: string | null;
}

export interface GuestRequest {
  fullName: string;
  gender: Gender | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  side: Side;
  relationship: string | null;
  category: GuestCategory;
  groupId: number | null;
  contactPerson: string | null;
  vip: boolean;
  note: string | null;
  invitations: InvitationInput[] | null;
  ignoreDuplicates?: boolean;
}

export interface GuestFilter {
  q?: string;
  side?: Side | null;
  groupId?: number | null;
  category?: GuestCategory | null;
  status?: InvitationStatus | null;
  eventId?: number | null;
  vip?: boolean | null;
  archived?: boolean | null;
  unassigned?: boolean | null;
  hasGift?: boolean | null;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface GuestStats {
  total: number;
  groom: number;
  bride: number;
  both: number;
  archived: number;
  notSent: number;
  sent: number;
  pending: number;
  confirmed: number;
  declined: number;
  attended: number;
  absent: number;
}

export interface DuplicateCheck {
  duplicate: boolean;
  matches: GuestSummary[];
}

export interface RowMessage {
  row: number;
  name: string | null;
  message: string;
}

export interface ImportResult {
  totalRows: number;
  imported: number;
  skipped: number;
  failed: number;
  skippedRows: RowMessage[];
  errors: RowMessage[];
}

export interface GuestGroup {
  id: number;
  name: string;
  side: Side;
  category: GuestCategory | null;
  representativeName: string | null;
  representativePhone: string | null;
  description: string | null;
  memberCount: number;
  totalInvited: number;
  totalConfirmed: number;
  totalActual: number;
  giftTotal: number;
  giftCount: number;
  createdAt: string;
}

export interface GuestGroupRequest {
  name: string;
  side: Side;
  category: GuestCategory | null;
  representativeName: string | null;
  representativePhone: string | null;
  description: string | null;
}

export interface InvitationUpdateRequest {
  status?: InvitationStatus | null;
  invitedCount?: number | null;
  expectedCount?: number | null;
  confirmedCount?: number | null;
  actualCount?: number | null;
  note?: string | null;
}

// ---- events -----------------------------------------------------------------

export interface EventStats {
  guestCount: number;
  groomGuests: number;
  brideGuests: number;
  invitedPeople: number;
  expectedPeople: number;
  confirmedPeople: number;
  actualPeople: number;
  confirmedGuests: number;
  declinedGuests: number;
  pendingGuests: number;
  attendedGuests: number;
  checkedInGuests: number;
  giftTotal: number;
  giftCount: number;
  expenseEstimated: number;
  expenseActual: number;
  expensePaid: number;
  tableCount: number;
  totalSeats: number;
  seatedPeople: number;
  unassignedGuests: number;
}

export interface WeddingEvent {
  id: number;
  name: string;
  eventDate: string;
  startTime: string | null;
  endTime: string | null;
  venue: string | null;
  address: string | null;
  side: Side;
  description: string | null;
  budget: number;
  status: EventStatus;
  main: boolean;
  sortOrder: number;
  daysUntil: number;
  stats: EventStats;
  createdAt: string;
  updatedAt: string;
}

export interface EventRequest {
  name: string;
  eventDate: string;
  startTime: string | null;
  endTime: string | null;
  venue: string | null;
  address: string | null;
  side: Side;
  description: string | null;
  budget: number;
  status: EventStatus;
  main: boolean;
  sortOrder?: number;
}

// ---- seating ----------------------------------------------------------------

export interface TableArea {
  id: number;
  eventId: number;
  name: string;
  description: string | null;
  sortOrder: number;
  tableCount: number;
}

export interface Seat {
  assignmentId: number;
  guestId: number;
  guestCode: string;
  guestName: string;
  phone: string | null;
  side: Side;
  groupName: string | null;
  seats: number;
  status: InvitationStatus | null;
  checkedIn: boolean;
  note: string | null;
}

export interface WeddingTable {
  id: number;
  eventId: number;
  areaId: number | null;
  areaName: string | null;
  name: string;
  capacity: number;
  side: Side;
  positionX: number;
  positionY: number;
  sortOrder: number;
  note: string | null;
  seatsTaken: number;
  seatsAvailable: number;
  seats: Seat[];
}

export interface TableRequest {
  eventId: number;
  areaId: number | null;
  name: string;
  capacity: number;
  side: Side;
  note: string | null;
  positionX?: number;
  positionY?: number;
  sortOrder?: number;
}

export interface UnassignedGuest {
  guestId: number;
  code: string;
  fullName: string;
  phone: string | null;
  side: Side;
  groupName: string | null;
  status: InvitationStatus;
  expectedCount: number;
  confirmedCount: number;
}

export interface SeatingOverview {
  eventId: number;
  eventName: string;
  tableCount: number;
  totalSeats: number;
  seatsTaken: number;
  seatedGuests: number;
  unassignedGuests: number;
  areas: TableArea[];
  tables: WeddingTable[];
}

export interface SeatLocation {
  guestId: number;
  guestName: string;
  tableId: number;
  tableName: string;
  areaName: string | null;
  seats: number;
}

// ---- check-in ---------------------------------------------------------------

export interface CheckinResult {
  alreadyCheckedIn: boolean;
  message: string;
  invitation: Invitation;
}

export interface CheckinStats {
  eventId: number;
  eventName: string;
  totalInvitations: number;
  checkedInGuests: number;
  checkedInPeople: number;
  expectedPeople: number;
  confirmedPeople: number;
  notArrived: number;
  declined: number;
  recent: Invitation[];
}

// ---- gifts ------------------------------------------------------------------

export interface GiftItem {
  id?: number;
  itemName: string;
  quantity: number;
  unit: string | null;
  estimatedValue: number | null;
  note?: string | null;
}

export interface Gift {
  id: number;
  guestId: number | null;
  guestCode: string | null;
  giverName: string;
  guestPhone: string | null;
  groupName: string | null;
  side: Side;
  eventId: number | null;
  eventName: string | null;
  giftType: GiftType;
  giftTypeLabel: string;
  amount: number | null;
  valueKnown: boolean;
  receivedAt: string;
  recordedBy: string | null;
  note: string | null;
  items: GiftItem[];
  itemsSummary: string;
  createdAt: string;
  updatedAt: string;
}

export interface GiftRequest {
  guestId: number | null;
  giverName: string | null;
  side: Side | null;
  eventId: number | null;
  giftType: GiftType;
  amount: number | null;
  receivedAt: string;
  note: string | null;
  items: GiftItem[];
}

export interface GiftFilter {
  q?: string;
  side?: Side | null;
  type?: GiftType | null;
  eventId?: number | null;
  guestId?: number | null;
  groupId?: number | null;
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface GiftTypeStat {
  type: GiftType;
  label: string;
  count: number;
  amount: number;
}

export interface GiftSideStat {
  side: Side;
  label: string;
  count: number;
  guests: number;
  amount: number;
}

export interface GiftEventStat {
  eventId: number | null;
  eventName: string;
  count: number;
  guests: number;
  amount: number;
}

export interface InKindSummary {
  itemName: string;
  unit: string | null;
  quantity: number;
  count: number;
  estimatedValue: number;
}

export interface GiftStats {
  totalAmount: number;
  cashAmount: number;
  transferAmount: number;
  inKindValue: number;
  giftCount: number;
  inKindCount: number;
  unknownValueCount: number;
  giverCount: number;
  guestsWithoutGift: number;
  activeGuests: number;
  averagePerGiver: number;
  averagePerGroup: number;
  groupsWithGifts: number;
  bySide: GiftSideStat[];
  byType: GiftTypeStat[];
  byEvent: GiftEventStat[];
  inKindItems: InKindSummary[];
  byCategory: Record<string, number>;
}

// ---- expenses ---------------------------------------------------------------

export interface ExpenseCategory {
  id: number;
  name: string;
  icon: string | null;
  budgetAmount: number;
  sortOrder: number;
  system: boolean;
  expenseCount: number;
  estimatedTotal: number;
  actualTotal: number;
  paidTotal: number;
  overBudget: boolean;
}

export interface ExpenseCategoryRequest {
  name: string;
  icon: string | null;
  budgetAmount: number;
  sortOrder?: number | null;
}

export interface Payment {
  id: number;
  expenseId: number;
  amount: number;
  paymentDate: string;
  paymentType: PaymentType;
  paymentTypeLabel: string;
  method: PaymentMethod;
  methodLabel: string;
  paidBy: string | null;
  note: string | null;
  createdAt: string;
}

export interface PaymentRequest {
  amount: number;
  paymentDate: string;
  paymentType: PaymentType;
  method: PaymentMethod;
  paidBy: string | null;
  note: string | null;
}

export interface Expense {
  id: number;
  name: string;
  categoryId: number;
  categoryName: string;
  categoryIcon: string | null;
  vendorId: number | null;
  vendorName: string | null;
  eventId: number | null;
  eventName: string | null;
  side: Side;
  estimatedAmount: number;
  actualAmount: number;
  paidAmount: number;
  depositAmount: number;
  remainingAmount: number;
  paymentStatus: PaymentStatus;
  paymentStatusLabel: string;
  overBudget: boolean;
  overBudgetAmount: number;
  expenseDate: string | null;
  dueDate: string | null;
  lastPaymentDate: string | null;
  overdue: boolean;
  paidBy: string | null;
  note: string | null;
  attachmentCount: number;
  payments: Payment[];
  createdAt: string;
  updatedAt: string;
}

export interface ExpenseRequest {
  name: string;
  categoryId: number;
  vendorId: number | null;
  eventId: number | null;
  side: Side;
  estimatedAmount: number;
  actualAmount: number;
  expenseDate: string | null;
  dueDate: string | null;
  paidBy: string | null;
  note: string | null;
  paidAmount?: number | null;
}

export interface ExpenseFilter {
  q?: string;
  categoryId?: number | null;
  vendorId?: number | null;
  eventId?: number | null;
  side?: Side | null;
  status?: PaymentStatus | null;
  overBudget?: boolean | null;
  unpaid?: boolean | null;
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}

export interface CategoryStat {
  categoryId: number;
  categoryName: string;
  icon: string | null;
  count: number;
  budget: number;
  estimated: number;
  actual: number;
  paid: number;
  remaining: number;
  percentOfTotal: number;
  overBudget: boolean;
}

export interface ExpenseSideStat {
  side: Side;
  label: string;
  count: number;
  estimated: number;
  actual: number;
  paid: number;
}

export interface VendorStat {
  vendorId: number;
  vendorName: string;
  count: number;
  actual: number;
  paid: number;
  remaining: number;
}

export interface StatusStat {
  status: PaymentStatus;
  label: string;
  count: number;
  actual: number;
}

export interface ExpenseStats {
  totalBudget: number;
  categoryBudgetTotal: number;
  estimatedTotal: number;
  actualTotal: number;
  paidTotal: number;
  remainingTotal: number;
  depositTotal: number;
  expenseCount: number;
  unpaidCount: number;
  overBudgetCount: number;
  overBudgetAmount: number;
  largestCategory: CategoryStat | null;
  byCategory: CategoryStat[];
  bySide: ExpenseSideStat[];
  byVendor: VendorStat[];
  byStatus: StatusStat[];
  overBudgetExpenses: Expense[];
}

export interface DebtItem {
  expenseId: number;
  expenseName: string;
  categoryName: string;
  vendorId: number | null;
  vendorName: string | null;
  actualAmount: number;
  paidAmount: number;
  remainingAmount: number;
  dueDate: string | null;
  status: PaymentStatus;
  overdue: boolean;
  daysUntilDue: number | null;
}

export interface DebtSummary {
  totalRemaining: number;
  count: number;
  overdueCount: number;
  overdueAmount: number;
  dueSoonCount: number;
  items: DebtItem[];
}

// ---- budget -----------------------------------------------------------------

export interface OtherIncome {
  id: number;
  source: string;
  side: Side;
  amount: number;
  receivedAt: string;
  method: PaymentMethod;
  note: string | null;
  createdAt: string;
}

export interface OtherIncomeRequest {
  source: string;
  side: Side;
  amount: number;
  receivedAt: string;
  method: PaymentMethod;
  note: string | null;
}

export interface BudgetAlert {
  type: string;
  severity: 'error' | 'warning' | 'info';
  message: string;
  referenceId: number | null;
  amount: number | null;
}

export interface SideFinance {
  side: Side;
  label: string;
  giftTotal: number;
  otherIncome: number;
  expenseActual: number;
  expensePaid: number;
  balance: number;
}

export interface BudgetOverview {
  totalBudget: number;
  categoryBudgetTotal: number;
  unallocatedBudget: number;
  estimatedTotal: number;
  actualTotal: number;
  paidTotal: number;
  remainingToPay: number;
  depositTotal: number;
  budgetRemaining: number;
  budgetUsedPercent: number;
  giftTotal: number;
  otherIncomeTotal: number;
  totalIncome: number;
  projectedBalance: number;
  actualBalance: number;
  cashBalance: number;
  cashIncome: number;
  transferIncome: number;
  cashExpense: number;
  transferExpense: number;
  categories: CategoryStat[];
  bySide: SideFinance[];
  alerts: BudgetAlert[];
}

// ---- vendors ----------------------------------------------------------------

export interface Vendor {
  id: number;
  name: string;
  serviceType: string | null;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  serviceDescription: string | null;
  agreedPrice: number | null;
  depositAmount: number;
  contractStatus: ContractStatus;
  contractStatusLabel: string;
  contractDate: string | null;
  rating: number | null;
  note: string | null;
  expenseCount: number;
  expenseActual: number;
  expensePaid: number;
  expenseRemaining: number;
  attachmentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface VendorRequest {
  name: string;
  serviceType: string | null;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  serviceDescription: string | null;
  agreedPrice: number | null;
  depositAmount: number;
  contractStatus: ContractStatus;
  contractDate: string | null;
  rating: number | null;
  note: string | null;
}

// ---- tasks ------------------------------------------------------------------

export interface Task {
  id: number;
  title: string;
  description: string | null;
  assignee: string | null;
  startDate: string | null;
  dueDate: string | null;
  priority: TaskPriority;
  priorityLabel: string;
  status: TaskStatus;
  statusLabel: string;
  effectiveStatus: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'OVERDUE';
  effectiveStatusLabel: string;
  overdue: boolean;
  daysUntilDue: number | null;
  completedAt: string | null;
  sortOrder: number;
  eventId: number | null;
  eventName: string | null;
  note: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  assignee: string | null;
  startDate: string | null;
  dueDate: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  eventId: number | null;
  note: string | null;
  sortOrder?: number | null;
}

export interface TaskStats {
  total: number;
  todo: number;
  inProgress: number;
  done: number;
  overdue: number;
  dueSoon: number;
  completionPercent: number;
}

// ---- dashboard --------------------------------------------------------------

export interface KeyFigures {
  totalGuests: number;
  groomGuests: number;
  brideGuests: number;
  bothGuests: number;
  totalInvitations: number;
  invitedPeople: number;
  expectedPeople: number;
  confirmedPeople: number;
  actualPeople: number;
  confirmedGuests: number;
  declinedGuests: number;
  undecidedGuests: number;
  attendedGuests: number;
  checkedInGuests: number;
  giftTotal: number;
  giftGroom: number;
  giftBride: number;
  giftCount: number;
  giverCount: number;
  otherIncomeTotal: number;
  totalIncome: number;
  expenseEstimated: number;
  expenseActual: number;
  expensePaid: number;
  expenseRemaining: number;
  totalBudget: number;
  budgetRemaining: number;
  balance: number;
  averageGiftPerGiver: number;
  averageGiftPerGroup: number;
  averageCostPerAttendee: number | null;
  tasksTotal: number;
  tasksDone: number;
  tasksOverdue: number;
  taskCompletionPercent: number;
  daysUntilWedding: number | null;
}

export interface LabelValue {
  key: string;
  label: string;
  count: number;
  amount: number | null;
}

export interface StatusSlice {
  status: InvitationStatus;
  label: string;
  guests: number;
  people: number;
}

export interface CategorySlice {
  categoryId: number;
  label: string;
  budget: number;
  estimated: number;
  actual: number;
  percent: number;
}

export interface TimePoint {
  period: string;
  from: string;
  income: number;
  giftIncome: number;
  expensePaid: number;
  cumulativeIncome: number;
  cumulativeExpense: number;
}

export interface DashboardCharts {
  guestsBySide: LabelValue[];
  guestsByStatus: StatusSlice[];
  giftsBySide: LabelValue[];
  giftsByType: LabelValue[];
  expensesByCategory: CategorySlice[];
  budgetVsActual: CategorySlice[];
  cashflow: TimePoint[];
  guestsByCategory: LabelValue[];
}

export interface PendingGuest {
  guestId: number;
  code: string;
  fullName: string;
  phone: string | null;
  side: Side;
  groupName: string | null;
  status: InvitationStatus;
  statusLabel: string;
  eventName: string | null;
  eventDate: string | null;
  daysSinceSent: number | null;
}

export interface DashboardLists {
  topExpenses: Expense[];
  unpaidExpenses: Expense[];
  pendingGuests: PendingGuest[];
  pendingGuestCount: number;
  guestsToContact: PendingGuest[];
  guestsToContactCount: number;
  upcomingTasks: Task[];
  unassignedGuests: GuestSummary[];
  unassignedGuestCount: number;
}

export interface EventBrief {
  id: number;
  name: string;
  eventDate: string;
  main: boolean;
  daysUntil: number;
}

export interface DashboardSummary {
  groomName: string;
  brideName: string;
  weddingDate: string | null;
  sideFilter: Side | null;
  eventFilter: number | null;
  from: string | null;
  to: string | null;
  figures: KeyFigures;
  charts: DashboardCharts;
  lists: DashboardLists;
  events: EventBrief[];
  alerts: BudgetAlert[];
}

export interface DashboardFilter {
  side?: Side | null;
  eventId?: number | null;
  from?: string | null;
  to?: string | null;
}

// ---- reports ----------------------------------------------------------------

export interface ReportHeader {
  groomName: string;
  brideName: string;
  weddingDate: string | null;
  generatedAt: string;
  sideLabel: string;
  eventName: string | null;
  from: string | null;
  to: string | null;
}

export interface CountRow {
  key: string;
  label: string;
  guests: number;
  invitedPeople: number;
  confirmedPeople: number;
  actualPeople: number;
  attendanceRate: number;
}

export interface GuestReport {
  header: ReportHeader;
  totalGuests: number;
  groomGuests: number;
  brideGuests: number;
  invitedPeople: number;
  expectedPeople: number;
  confirmedPeople: number;
  actualPeople: number;
  attendedGuests: number;
  declinedGuests: number;
  undecidedGuests: number;
  attendanceRatePeople: number;
  attendanceRateGuests: number;
  bySide: CountRow[];
  byCategory: CountRow[];
  byStatus: CountRow[];
  byEvent: CountRow[];
}

export interface GiftSideRow {
  side: Side;
  label: string;
  count: number;
  givers: number;
  amount: number;
  percent: number;
}

export interface GiftReport {
  header: ReportHeader;
  totalAmount: number;
  giftCount: number;
  giverCount: number;
  cashAmount: number;
  transferAmount: number;
  inKindValue: number;
  inKindCount: number;
  averagePerGiver: number;
  bySide: GiftSideRow[];
  byType: GiftTypeStat[];
  byEvent: GiftEventStat[];
  details: Gift[];
}

export interface ExpenseReport {
  header: ReportHeader;
  totalBudget: number;
  estimatedTotal: number;
  actualTotal: number;
  paidTotal: number;
  remainingTotal: number;
  expenseCount: number;
  overBudgetCount: number;
  overBudgetAmount: number;
  byCategory: CategoryStat[];
  bySide: ExpenseSideStat[];
  byVendor: VendorStat[];
  byStatus: StatusStat[];
  overBudget: Expense[];
  unpaid: Expense[];
  details: Expense[];
}

export interface SideFinanceRow {
  side: Side;
  label: string;
  guests: number;
  actualPeople: number;
  giftTotal: number;
  otherIncome: number;
  expenseActual: number;
  balance: number;
}

export interface FinancialReport {
  header: ReportHeader;
  totalBudget: number;
  giftTotal: number;
  otherIncomeTotal: number;
  totalIncome: number;
  expenseEstimated: number;
  expenseActual: number;
  expensePaid: number;
  expenseRemaining: number;
  balance: number;
  projectedBalance: number;
  budgetRemaining: number;
  actualAttendees: number;
  giverCount: number;
  costPerAttendee: number | null;
  giftPerGiver: number | null;
  cashIn: number;
  transferIn: number;
  cashOut: number;
  transferOut: number;
  bySide: SideFinanceRow[];
  otherIncomes: OtherIncome[];
}

export interface ReportFilter {
  side?: Side | null;
  eventId?: number | null;
  from?: string | null;
  to?: string | null;
}

// ---- misc -------------------------------------------------------------------

export interface Attachment {
  id: number;
  entityType: AttachmentEntityType;
  entityId: number;
  originalName: string;
  contentType: string | null;
  sizeBytes: number;
  description: string | null;
  uploadedBy: string | null;
  downloadUrl: string;
  image: boolean;
  createdAt: string;
}

export interface SearchHit {
  type: 'GUEST' | 'GIFT' | 'EXPENSE' | 'VENDOR' | 'TASK';
  id: number;
  title: string;
  subtitle: string;
  route: string;
  side: Side | null;
  amount: number | null;
}

export interface SearchResponse {
  query: string;
  guests: SearchHit[];
  gifts: SearchHit[];
  expenses: SearchHit[];
  vendors: SearchHit[];
  tasks: SearchHit[];
  total: number;
}

export interface Reminder {
  type: string;
  severity: 'error' | 'warning' | 'info';
  title: string;
  message: string;
  route: string;
  referenceId: number | null;
  date: string | null;
  amount: number | null;
}

export interface ReminderSummary {
  total: number;
  critical: number;
  warnings: number;
  items: Reminder[];
}

export interface AuditLog {
  id: number;
  entityType: string;
  entityId: number | null;
  entityLabel: string | null;
  action: string;
  summary: string | null;
  changes: Record<string, { from: unknown; to: unknown }> | null;
  performedBy: string | null;
  performedAt: string;
}

export interface BackupInfo {
  version: string;
  generatedAt: string;
  tables: string[];
  totalRows: number;
  fileName: string;
}

export interface RestoreResult {
  success: boolean;
  message: string;
  restoredRows: number;
  preRestoreBackupFile: string;
  tables: string[];
}
