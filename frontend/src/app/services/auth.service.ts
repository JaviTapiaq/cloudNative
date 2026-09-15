import { Injectable, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { Router } from '@angular/router';
import { loginRequest } from '../configs/msal.config';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private msalService = inject(MsalService);
  private router = inject(Router);

  login(): void {
    this.msalService.loginRedirect(loginRequest);
  }

  logout(): void {
    this.msalService.logoutRedirect({
      postLogoutRedirectUri: 'http://localhost:4200/login',
    });
  }

  isLoggedIn(): boolean {
    return this.msalService.instance.getActiveAccount() !== null;
  }

  getUserName(): string | null {
    const account = this.msalService.instance.getActiveAccount();
    return account?.name ?? account?.username ?? null;
  }

  getRoles(): string[] {
    const account = this.msalService.instance.getActiveAccount();
    if (!account?.idTokenClaims) {
      return [];
    }
    const roles = (account.idTokenClaims as Record<string, unknown>)['roles'];
    return Array.isArray(roles) ? roles as string[] : [];
  }

  handleRedirect(): void {
    this.msalService.handleRedirectObservable().subscribe();
  }
}
