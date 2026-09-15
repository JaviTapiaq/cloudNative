import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { NavbarComponent } from './components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private authService = inject(AuthService);
  title = 'Pedidos360';

  ngOnInit(): void {
    this.authService.handleRedirect();
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
