import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthSession } from '@harborpath/shared';

@Component({
  selector: 'hp-shell-layout',
  imports: [RouterOutlet, RouterLink],
  template: `
    @if (session.apiDown()) {
      <div class="banner" role="status">Recordkeeping service unavailable.</div>
    }
    <header class="top">
      <a class="brand" routerLink="/">
        <strong>Harborpath</strong>
        <small>Recordkeeping</small>
      </a>
      <nav>
        @if (session.role() === 'PARTICIPANT') {
          <a routerLink="/participant">Household</a>
        }
        @if (session.role() === 'SPONSOR') {
          <a routerLink="/sponsor">Plan book</a>
        }
        <a routerLink="/ledger">Ledger</a>
      </nav>
      <div class="session">
        <span>{{ session.displayName() }} · {{ session.role() === 'SPONSOR' ? 'Sponsor' : 'Participant' }}</span>
        <button type="button" (click)="signOut()">Sign out</button>
      </div>
    </header>
    <div class="stage">
      <div class="perf" aria-hidden="true"></div>
      <router-outlet />
    </div>
  `,
  styles: [`
    .banner { background: var(--ribbon); color: #fff; text-align: center; padding: .45rem; font-size: .9rem; }
    .top {
      display: flex; align-items: center; gap: 1.5rem; padding: .85rem 1.4rem;
      border-bottom: 1px solid var(--grid); background: var(--paper);
    }
    .brand { display: flex; flex-direction: column; text-decoration: none; line-height: 1.1; }
    .brand strong { font-family: Sora, sans-serif; font-size: 1.15rem; }
    .brand small { font-size: .7rem; letter-spacing: .12em; text-transform: uppercase; color: var(--muted); }
    nav { display: flex; gap: 1rem; flex: 1; }
    nav a { text-decoration: none; font-size: .95rem; }
    .session { display: flex; gap: .8rem; align-items: center; font-size: .9rem; }
    .session button { background: transparent; border: 1px solid var(--grid); padding: .3rem .6rem; cursor: pointer; }
    .stage { position: relative; min-height: calc(100vh - 4rem); }
    .perf {
      position: absolute; left: 1.15rem; top: 0; bottom: 0; width: 8px;
      background-image: radial-gradient(circle, var(--grid) 1.2px, transparent 1.3px);
      background-size: 8px 10px; pointer-events: none;
    }
  `]
})
export class ShellLayoutComponent {
  readonly session = inject(AuthSession);
  private readonly router = inject(Router);

  signOut(): void {
    this.session.clear();
    void this.router.navigateByUrl('/login');
  }
}
