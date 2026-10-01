import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { API_BASE, AccountSummary, HouseholdResponse, money, pct } from '@harborpath/shared';

@Component({
  selector: 'hp-household',
  imports: [RouterLink],
  template: `
    <section class="page">
      <p class="eyebrow">Plan year 2026</p>
      <h1>{{ household()?.participantName }} household</h1>
      <p class="lede">Balances on file. Open an account to change a deferral or review beneficiaries.</p>

      @if (gap(); as g) {
        <p class="notice" role="status">
          {{ g.name }} has no primary beneficiary.
          <a [routerLink]="['accounts', g.id, 'beneficiaries']">Fix designations</a>
          — that opens an item on the sponsor compliance queue.
        </p>
      }

      <div class="cards">
        @for (account of household()?.accounts ?? []; track account.id) {
          <a class="card" [routerLink]="['accounts', account.id]">
            <span class="kind">{{ account.type === 'EDUCATION_529' ? '529' : '401(k)' }}</span>
            <strong>{{ account.name }}</strong>
            <em>{{ money(account.balance) }}</em>
            @if (account.deferralPercent !== null) {
              <span>Deferral {{ pct(account.deferralPercent) }}</span>
            }
          </a>
        }
      </div>

      @if (household(); as h) {
        <aside class="tape" aria-label="Contribution tape">
          <span>YTD EMPLOYEE {{ money(h.tape.ytdEmployee) }}</span>
          <span>YTD MATCH {{ money(h.tape.ytdMatch) }}</span>
        </aside>
      }
    </section>
  `,
  styles: [`
    .page { padding: 2rem 2.5rem 6rem; max-width: 64rem; }
    .eyebrow { letter-spacing: .16em; text-transform: uppercase; font-size: .72rem; color: var(--muted); }
    h1 { font-family: Sora, sans-serif; font-size: 2rem; margin: .2rem 0; }
    .lede { color: var(--muted); }
    .notice {
      background: var(--paper); border: 1px solid var(--ribbon); padding: .75rem 1rem; margin-top: 1rem;
    }
    .notice a { color: var(--folder); }
    .cards { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1.5rem; }
    .card {
      background: var(--paper); border: 1px solid var(--grid); padding: 1.2rem;
      text-decoration: none; display: grid; gap: .35rem;
    }
    .kind { font-size: .7rem; background: var(--tab); width: fit-content; padding: .1rem .4rem; }
    .card em { font-style: normal; font-family: "IBM Plex Mono", monospace; font-size: 1.4rem; }
    .tape {
      margin-top: 2rem; font-family: "IBM Plex Mono", monospace; font-size: .85rem;
      border-top: 1px dashed var(--grid); padding-top: .8rem; display: flex; gap: 2rem;
    }
  `]
})
export class HouseholdComponent implements OnInit {
  private readonly http = inject(HttpClient);
  household = signal<HouseholdResponse | null>(null);
  money = money;
  pct = pct;

  gap(): AccountSummary | undefined {
    return this.household()?.accounts.find((a) => a.type === 'RETIREMENT_401K' && !a.beneficiaryComplete);
  }

  ngOnInit(): void {
    this.http.get<HouseholdResponse>(`${API_BASE}/households/me`).subscribe((h) => this.household.set(h));
  }

  accountPath(account: AccountSummary): string[] {
    return ['accounts', account.id];
  }
}
