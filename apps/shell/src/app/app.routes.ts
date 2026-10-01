import { inject } from '@angular/core';
import { Router, Routes } from '@angular/router';
import { loadRemoteModule } from '@angular-architects/native-federation';
import { AuthSession } from '@harborpath/shared';
import { authGuard, roleGuard } from './auth.guard';
import { ForbiddenComponent } from './forbidden.component';
import { LoginComponent } from './login.component';
import { ShellLayoutComponent } from './shell-layout.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'forbidden', component: ForbiddenComponent },
  {
    path: '',
    component: ShellLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'participant',
        canActivate: [roleGuard('PARTICIPANT')],
        loadChildren: () =>
          loadRemoteModule('participant', './App').then((m) => m.routes)
      },
      {
        path: 'ledger',
        loadChildren: () =>
          loadRemoteModule('contributions', './App').then((m) => m.routes)
      },
      {
        path: 'sponsor',
        canActivate: [roleGuard('SPONSOR')],
        loadChildren: () =>
          loadRemoteModule('sponsor', './App').then((m) => m.routes)
      },
      {
        path: '',
        pathMatch: 'full',
        canActivate: [
          () => inject(Router).createUrlTree([inject(AuthSession).homePath()])
        ],
        component: ShellLayoutComponent
      }
    ]
  }
];
