import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'hp-forbidden',
  imports: [RouterLink],
  template: `
    <main class="page">
      <h1>This chair isn’t yours.</h1>
      <p>That seat is reserved for the other role. Sign in with the matching persona, or go back to your book of record.</p>
      <a routerLink="/login">Return to sign-in</a>
    </main>
  `,
  styles: [`
    .page { max-width: 36rem; margin: 5rem auto; padding: 0 1.5rem; }
    h1 { font-family: Sora, sans-serif; }
    a { color: var(--folder); }
  `]
})
export class ForbiddenComponent {}
