import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { API_BASE, AuthSession, LoginResponse } from '@harborpath/shared';

@Component({
  selector: 'hp-login',
  imports: [FormsModule],
  template: `
    <main class="login">
      <p class="eyebrow">Harborpath Recordkeeping</p>
      <h1>Choose a chair</h1>
      <p class="lede">Same plan year. Two seats. Sign in as the saver or the sponsor.</p>

      <div class="cards">
        <button type="button" class="persona" (click)="fill('maya.chen')">
          <span class="tab">Participant</span>
          <strong>Maya Chen</strong>
          <span>401(k) + Avery’s 529</span>
        </button>
        <button type="button" class="persona" (click)="fill('jordan.ellis')">
          <span class="tab">Sponsor</span>
          <strong>Jordan Ellis</strong>
          <span>Harborpath Staff 401(k)</span>
        </button>
      </div>

      <form (ngSubmit)="submit()">
        <label>Username
          <input name="username" [(ngModel)]="username" autocomplete="username" />
        </label>
        <label>Password
          <input name="password" [(ngModel)]="password" type="password" autocomplete="current-password" />
        </label>
        @if (error()) {
          <p class="err" role="alert">{{ error() }}</p>
        }
        <button class="primary" type="submit">Sign in</button>
      </form>
    </main>
  `,
  styles: [`
    .login { max-width: 34rem; margin: 4rem auto; padding: 0 1.5rem; }
    .eyebrow { letter-spacing: .18em; text-transform: uppercase; font-size: .72rem; color: var(--muted); }
    h1 { font-family: Sora, sans-serif; font-weight: 600; font-size: 2.4rem; margin: .2rem 0 0; }
    .lede { color: var(--muted); margin-bottom: 1.75rem; }
    .cards { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; margin-bottom: 1.5rem; }
    .persona {
      text-align: left; background: var(--paper); border: 1px solid var(--grid);
      border-radius: 2px; padding: 1rem; cursor: pointer;
    }
    .persona:hover { border-color: var(--folder); }
    .tab { display: inline-block; background: var(--tab); font-size: .7rem; padding: .1rem .45rem; margin-bottom: .5rem; }
    .persona strong { display: block; font-family: Sora, sans-serif; }
    .persona span { color: var(--muted); font-size: .9rem; }
    form { display: grid; gap: .8rem; }
    label { display: grid; gap: .3rem; font-size: .85rem; }
    input { border: 1px solid var(--grid); background: var(--paper); padding: .55rem .7rem; }
    .primary { background: var(--folder); color: var(--folder-ink); border: 0; padding: .7rem 1rem; cursor: pointer; }
    .err { color: var(--ribbon); margin: 0; }
  `]
})
export class LoginComponent {
  private readonly http = inject(HttpClient);
  private readonly session = inject(AuthSession);
  private readonly router = inject(Router);

  username = '';
  password = 'harborpath';
  error = signal<string | null>(null);

  fill(username: string): void {
    this.username = username;
    this.password = 'harborpath';
  }

  submit(): void {
    this.error.set(null);
    this.http.post<LoginResponse>(`${API_BASE}/auth/login`, {
      username: this.username,
      password: this.password
    }).subscribe({
      next: (res) => {
        this.session.set(res);
        void this.router.navigateByUrl(this.session.homePath());
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.status === 401
          ? 'That sign-in didn’t match our records.'
          : 'Recordkeeping service unavailable.');
      }
    });
  }
}
