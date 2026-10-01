import { Routes } from '@angular/router';
import { AccountComponent } from './account.component';
import { BeneficiariesComponent } from './beneficiaries.component';
import { HouseholdComponent } from './household.component';

export const routes: Routes = [
  { path: '', component: HouseholdComponent },
  { path: 'accounts/:id', component: AccountComponent },
  { path: 'accounts/:id/beneficiaries', component: BeneficiariesComponent }
];
