import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TASK_PRIORITY_CLASS, TASK_PRIORITY_OPTIONS, TASK_STATUS_OPTIONS, TaskPriority, TaskStatus } from '../../core/models/enums';
import { Task, TaskRequest, TaskStats, WeddingEvent } from '../../core/models/models';
import { EventService } from '../../core/services/event.service';
import { TaskService } from '../../core/services/misc.services';
import { NotificationService, extractMessage } from '../../core/services/notification.service';
import { fromIsoDate, toIsoDate } from '../../core/utils/vn-date-adapter';
import { ConfirmService } from '../../shared/components/confirm-dialog.component';
import { StatCardComponent } from '../../shared/components/stat-card.component';
import { ErrorStateComponent, LoadingComponent } from '../../shared/components/state.components';
import { ViDatePipe } from '../../shared/pipes/vi-date.pipe';

@Component({
  selector: 'app-task-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatDatepickerModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">{{ data.task ? 'Sửa công việc' : 'Thêm công việc' }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="form-grid">
        <mat-form-field class="full">
          <mat-label>Tên công việc</mat-label>
          <input matInput formControlName="title" required />
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Mô tả</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Người phụ trách</mat-label>
          <input matInput formControlName="assignee" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ưu tiên</mat-label>
          <mat-select formControlName="priority">
            @for (o of priorities; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Trạng thái</mat-label>
          <mat-select formControlName="status">
            @for (o of statuses; track o.value) { <mat-option [value]="o.value">{{ o.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Sự kiện</mat-label>
          <mat-select formControlName="eventId">
            <mat-option [value]="null">Không gắn</mat-option>
            @for (e of data.events; track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ngày bắt đầu</mat-label>
          <input matInput [matDatepicker]="s" formControlName="startDate" />
          <mat-datepicker-toggle matIconSuffix [for]="s"></mat-datepicker-toggle>
          <mat-datepicker #s></mat-datepicker>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Hạn hoàn thành</mat-label>
          <input matInput [matDatepicker]="d" formControlName="dueDate" />
          <mat-datepicker-toggle matIconSuffix [for]="d"></mat-datepicker-toggle>
          <mat-datepicker #d></mat-datepicker>
        </mat-form-field>
        <mat-form-field class="full">
          <mat-label>Ghi chú</mat-label>
          <textarea matInput formControlName="note" rows="2"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Hủy</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">Lưu</button>
      </mat-dialog-actions>
    </form>
  `
})
export class TaskFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ task?: Task; events: WeddingEvent[] }>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<TaskFormDialogComponent>);
  readonly priorities = TASK_PRIORITY_OPTIONS;
  readonly statuses = TASK_STATUS_OPTIONS;
  readonly form = this.fb.group({
    title: this.fb.nonNullable.control(this.data.task?.title ?? '', Validators.required),
    description: this.fb.control<string | null>(this.data.task?.description ?? null),
    assignee: this.fb.control<string | null>(this.data.task?.assignee ?? null),
    startDate: this.fb.control<Date | null>(fromIsoDate(this.data.task?.startDate)),
    dueDate: this.fb.control<Date | null>(fromIsoDate(this.data.task?.dueDate)),
    priority: this.fb.nonNullable.control<TaskPriority>(this.data.task?.priority ?? 'MEDIUM'),
    status: this.fb.nonNullable.control<TaskStatus>(this.data.task?.status ?? 'TODO'),
    eventId: this.fb.control<number | null>(this.data.task?.eventId ?? null),
    note: this.fb.control<string | null>(this.data.task?.note ?? null)
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: TaskRequest = {
      title: v.title.trim(),
      description: v.description?.trim() || null,
      assignee: v.assignee?.trim() || null,
      startDate: toIsoDate(v.startDate),
      dueDate: toIsoDate(v.dueDate),
      priority: v.priority,
      status: v.status,
      eventId: v.eventId,
      note: v.note?.trim() || null
    };
    this.ref.close(request);
  }
}

@Component({
  selector: 'app-task-board',
  standalone: true,
  imports: [
    DragDropModule,
    MatButtonModule,
    MatIconModule,
    StatCardComponent,
    LoadingComponent,
    ErrorStateComponent,
    ViDatePipe
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Chuẩn bị</div>
          <h1>Công việc chuẩn bị</h1>
          <div class="subtitle">Kéo thẻ giữa các cột. Công việc quá hạn được đánh dấu đỏ.</div>
        </div>
        <div class="actions">
          <button mat-flat-button color="primary" (click)="openForm()">
            <mat-icon class="material-symbols-rounded">add</mat-icon> Thêm công việc
          </button>
        </div>
      </div>

      @if (stats(); as s) {
        <div class="grid cols-5" style="margin-bottom: 18px">
          <app-stat-card label="Tổng việc" [value]="s.total" icon="checklist" />
          <app-stat-card label="Chưa làm" [value]="s.todo" icon="radio_button_unchecked" tone="info" />
          <app-stat-card label="Đang làm" [value]="s.inProgress" icon="pending" tone="warning" />
          <app-stat-card label="Hoàn thành" [value]="s.done" icon="check_circle" tone="success" />
          <app-stat-card label="Quá hạn" [value]="s.overdue" icon="alarm" tone="danger" [sub]="s.completionPercent + '% hoàn thành'" />
        </div>
        <div class="progress" style="margin-bottom: 18px"><div class="bar bar-primary" [style.width.%]="s.completionPercent"></div></div>
      }

      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [message]="error()!" (retry)="load()" /> }
      @else {
        <div class="board">
          @for (col of columns; track col.status) {
            <div class="col card flush">
              <div class="col-head">{{ col.label }} ({{ lists[col.status].length }})</div>
              <div class="col-body" cdkDropList [id]="col.status" [cdkDropListData]="lists[col.status]" [cdkDropListConnectedTo]="connected" (cdkDropListDropped)="drop($event, col.status)">
                @for (t of lists[col.status]; track t.id) {
                  <div class="task" cdkDrag [class.overdue]="t.overdue && t.status !== 'DONE'" (dblclick)="openForm(t)">
                    <div class="task-title">{{ t.title }}</div>
                    <div class="task-meta">
                      <span [class]="'chip ' + priClass(t.priority)">{{ t.priorityLabel }}</span>
                      @if (t.dueDate) { <span class="muted">{{ t.dueDate | viDate }}</span> }
                      @if (t.assignee) { <span class="muted">{{ t.assignee }}</span> }
                    </div>
                    <div class="task-act">
                      <button mat-icon-button (click)="openForm(t)"><mat-icon class="material-symbols-rounded">edit</mat-icon></button>
                      <button mat-icon-button (click)="remove(t)"><mat-icon class="material-symbols-rounded">delete</mat-icon></button>
                    </div>
                  </div>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .board { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; align-items: start; }
      .col-head { padding: 14px 16px; font-weight: 600; background: var(--wm-cream); }
      .col-body { min-height: 240px; padding: 12px; display: flex; flex-direction: column; gap: 10px; }
      .task { background: var(--wm-surface-2); border: 1px solid var(--wm-border); border-radius: 12px; padding: 12px; cursor: grab; }
      .task.overdue { border-color: var(--wm-danger); background: var(--wm-danger-bg); }
      .task-title { font-weight: 600; }
      .task-meta { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; font-size: 12px; }
      .task-act { display: flex; justify-content: flex-end; }
      @media (max-width: 900px) { .board { grid-template-columns: 1fr; } }
    `
  ]
})
export class TaskBoardComponent implements OnInit {
  private readonly service = inject(TaskService);
  private readonly eventsApi = inject(EventService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotificationService);
  private readonly confirm = inject(ConfirmService);

  readonly columns: { status: TaskStatus; label: string }[] = [
    { status: 'TODO', label: 'Chưa thực hiện' },
    { status: 'IN_PROGRESS', label: 'Đang thực hiện' },
    { status: 'DONE', label: 'Hoàn thành' }
  ];
  readonly connected = ['TODO', 'IN_PROGRESS', 'DONE'];
  lists: Record<TaskStatus, Task[]> = { TODO: [], IN_PROGRESS: [], DONE: [] };
  readonly stats = signal<TaskStats | null>(null);
  readonly events = signal<WeddingEvent[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.eventsApi.list().subscribe({ next: (e) => this.events.set(e) });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list().subscribe({
      next: (tasks) => {
        this.lists = { TODO: [], IN_PROGRESS: [], DONE: [] };
        for (const t of tasks) {
          const col: TaskStatus = t.status === 'DONE' ? 'DONE' : t.status === 'IN_PROGRESS' ? 'IN_PROGRESS' : 'TODO';
          this.lists[col].push(t);
        }
        this.loading.set(false);
      },
      error: (err) => { this.error.set(extractMessage(err)); this.loading.set(false); }
    });
    this.service.stats().subscribe({ next: (s) => this.stats.set(s) });
  }

  priClass(p: TaskPriority): string {
    return TASK_PRIORITY_CLASS[p];
  }

  drop(event: CdkDragDrop<Task[]>, status: TaskStatus): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      this.service.reorder(event.container.data.map((t) => t.id), status).subscribe({ error: (err) => this.notify.httpError(err) });
      return;
    }
    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    const task = event.container.data[event.currentIndex];
    this.service.changeStatus(task.id, status, event.currentIndex).subscribe({
      next: () => this.load(),
      error: (err) => { this.notify.httpError(err); this.load(); }
    });
  }

  openForm(task?: Task): void {
    this.dialog.open(TaskFormDialogComponent, { data: { task, events: this.events() }, width: '560px' }).afterClosed().subscribe((req?: TaskRequest) => {
      if (!req) return;
      const call = task ? this.service.update(task.id, req) : this.service.create(req);
      call.subscribe({
        next: () => { this.notify.success(task ? 'Đã cập nhật công việc' : 'Đã thêm công việc'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }

  remove(t: Task): void {
    this.confirm.delete(`công việc "${t.title}"`).subscribe((ok) => {
      if (!ok) return;
      this.service.delete(t.id).subscribe({
        next: () => { this.notify.success('Đã xóa công việc'); this.load(); },
        error: (err) => this.notify.httpError(err)
      });
    });
  }
}
