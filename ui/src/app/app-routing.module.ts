import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { AuthGuard } from './guards/auth.guard';
import {ProfileComponent} from "./components/profile/profile.component";
import {ShopsComponent} from "./components/shops/shops.component";
import {ShopFormComponent} from "./components/shop-form/shop-form.component";

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'dashboard', component: ShopsComponent, canActivate: [AuthGuard] },
  { path: 'customers', component: ShopsComponent, canActivate: [AuthGuard] },
  { path: 'suppliers', component: ShopsComponent, canActivate: [AuthGuard] },
  { path: 'ledger', component: ShopFormComponent, canActivate: [AuthGuard] },
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

