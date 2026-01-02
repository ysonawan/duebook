import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { AuthGuard } from './guards/auth.guard';
import {ProfileComponent} from "./components/profile/profile.component";
import {ShopsComponent} from "./components/shops/shops.component";
import {ShopFormComponent} from "./components/shop-form/shop-form.component";
import {CustomersComponent} from "./components/customers/customers.component";
import {CustomerFormComponent} from "./components/customer-form/customer-form.component";
import {LedgerComponent} from "./components/ledger/ledger.component";
import {LedgerFormComponent} from "./components/ledger-form/ledger-form.component";
import {DashboardDuebookComponent} from "./components/dashboard/dashboard-duebook.component";
import {SuppliersComponent} from "./components/suppliers/suppliers.component";

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'dashboard', component: DashboardDuebookComponent, canActivate: [AuthGuard] },
  { path: 'customers', component: CustomersComponent, canActivate: [AuthGuard] },
  { path: 'customers/new', component: CustomerFormComponent, canActivate: [AuthGuard] },
  { path: 'customers/edit/:id', component: CustomerFormComponent, canActivate: [AuthGuard] },
  { path: 'ledger', component: LedgerComponent, canActivate: [AuthGuard] },
  { path: 'ledger/new', component: LedgerFormComponent, canActivate: [AuthGuard] },
  { path: 'suppliers', component: SuppliersComponent, canActivate: [AuthGuard] },
  { path: 'shops', component: ShopsComponent, canActivate: [AuthGuard] },
  { path: 'shops/new', component: ShopFormComponent, canActivate: [AuthGuard] },
  { path: 'shops/edit/:id', component: ShopFormComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

