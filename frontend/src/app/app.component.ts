import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType, InteractionStatus } from '@azure/msal-browser';
import { filter, take } from 'rxjs/operators';
import { AuthService } from './services/auth.service';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private msalService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);
  private authService = inject(AuthService);

  title = 'Pedidos360';

  ngOnInit(): void {
    // 1. Procesa la respuesta de Azure al volver a localhost:4200
    this.msalService.handleRedirectObservable().subscribe({
      next: (result) => {
        if (result && result.account) {
          // Asigna la cuenta como activa en la instancia de MSAL
          this.msalService.instance.setActiveAccount(result.account);
          this.router.navigate(['/home']);
        } else {
          // Si la página se recargó y ya había un login guardado en cache
          const currentAccounts = this.msalService.instance.getAllAccounts();
          if (currentAccounts.length > 0) {
            this.msalService.instance.setActiveAccount(currentAccounts[0]);
          }
        }
      },
      error: (error) => console.error('[MSAL Error]', error)
    });

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGIN_SUCCESS),
        take(1)
      )
      .subscribe((msg: EventMessage) => {
        const payload = msg.payload as { account?: object };
        if (payload?.account) {
          this.msalService.instance.setActiveAccount(payload.account as any);
          this.router.navigate(['/home']);
        }
      });
  }

  get isLoggedIn() {
    return this.authService.isLoggedIn();
  }

  get userName() {
    return this.authService.getUserName();
  }

  logout(): void {
    this.authService.logout();
  }
}
