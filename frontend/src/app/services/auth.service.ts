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
    const activeAccount = this.msalService.instance.getActiveAccount();
    if (activeAccount) {
      return true;
    }

    // Respaldo por si se recarga la página y aún no se ha marcado activeAccount
    const accounts = this.msalService.instance.getAllAccounts();
    if (accounts.length > 0) {
      this.msalService.instance.setActiveAccount(accounts[0]);
      return true;
    }

    return false;
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
}
