import { NgModule, LOCALE_ID } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideZoneChangeDetection } from '@angular/core';
import { NgxEchartsModule } from 'ngx-echarts';
import { ToastrModule } from 'ngx-toastr';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { ProfileComponent } from './components/profile/profile.component';
import { ShopsComponent } from './components/shops/shops.component';
import { ShopFormComponent } from './components/shop-form/shop-form.component';

import { ShopService } from './services/shop.service';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { HttpErrorInterceptor } from './interceptors/http-error.interceptor';
import {CustomersComponent} from "./components/customers/customers.component";
import {CustomerFormComponent} from "./components/customer-form/customer-form.component";
import {LedgerComponent} from "./components/ledger/ledger.component";
import {LedgerFormComponent} from "./components/ledger-form/ledger-form.component";
import {DashboardDuebookComponent} from "./components/dashboard/dashboard-duebook.component";
import {SuppliersComponent} from "./components/suppliers/suppliers.component";
import {AuditLogsComponent} from "./components/audit-logs/audit-logs.component";

@NgModule({
    declarations: [
        AppComponent,
        LoginComponent,
        SignupComponent,
        ProfileComponent,
        ShopsComponent,
        ShopFormComponent,
        CustomersComponent,
        CustomerFormComponent,
        SuppliersComponent,
        LedgerComponent,
        LedgerFormComponent,
        DashboardDuebookComponent,
        AuditLogsComponent
    ],
    bootstrap: [AppComponent],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        AppRoutingModule,
        FormsModule,
        ReactiveFormsModule,
        NgxEchartsModule.forRoot({
            echarts: () => import('echarts')
        }),
        ToastrModule.forRoot({
            timeOut: 5000,
            positionClass: 'toast-top-right',
            preventDuplicates: true,
            closeButton: true,
            progressBar: true
        }),
    ],
    providers: [
        provideZoneChangeDetection({ eventCoalescing: false }),
        ShopService,
        AuthService,
        NotificationService,
        // AuthInterceptor runs first to handle auth-specific errors
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true
        },
        // HttpErrorInterceptor runs after to handle all other errors
        {
            provide: HTTP_INTERCEPTORS,
            useClass: HttpErrorInterceptor,
            multi: true
        },
        {
            provide: LOCALE_ID,
            useValue: 'en-IN'
        },
        provideHttpClient(withInterceptorsFromDi())
    ]
})
export class AppModule { }
