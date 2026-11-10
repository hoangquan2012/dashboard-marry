import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Tổng quan'
      },
      {
        path: 'guests',
        loadComponent: () => import('./features/guests/guest-list.component').then((m) => m.GuestListComponent),
        title: 'Khách mời'
      },
      { path: 'gifts', redirectTo: 'guests' },
      {
        path: 'expenses',
        loadComponent: () => import('./features/expenses/expense-list.component').then((m) => m.ExpenseListComponent),
        title: 'Chi phí'
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
