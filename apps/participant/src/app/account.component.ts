import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AccountSummary, API_BASE, LoanRow, money, pct } from '@harborpath/shared';

@Component({
  selector: 'hp-account',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="page">
      <a routerLink="/participant">Household</a>
      @if (account(); as a) {
        <p class="eyebrow">{{ a.type === 'EDUCATION_529' ? 'Education savings' : 'Retirement' }}</p>
        <h1>{{ a.name }}</h1>
        <p class="bal">{{ money(a.balance) }}</p>

        @if (a.type === 'RETIREMENT_401K') {
          <form (ngSubmit)="save()">
            <label>Elective deferral
              <input type="number" min="0" max="50" step="0.01" [(ngModel)]="percent" name="percent" />
            </label>
            @if (error()) {
              <p class="err" role="alert">{{ error() }}</p>
            }
            <button class="primary" type="submit">Save deferral</button>
          </form>

          <form class="loan" (ngSubmit)="requestLoan()">
            <h2>401(k) loan</h2>
            <p class="note">Lesser of 50% of balance or $50,000. Sponsor must approve before cash leaves the account.</p>
            @if (pending(); as loan) {
              <p class="ok">{{ money(loan.amount) }} {{ loan.status === 'REQUESTED' ? 'awaiting sponsor' : loan.status }}</p>
            } @else {
              <label>Amount
                <input type="number" min="1" step="0.01" name="loanAmount" [(ngModel)]="loanAmount" />
              </label>
              @if (loanError()) {
                <p class="err" role="alert">{{ loanError() }}</p>
              }
              <button class="primary" type="submit">Request loan</button>
            }
          </form>
        } @else {
          <p class="note">Owned for Avery Chen. Deferral does not apply to 529 accounts.</p>
        }

        <p><a [routerLink]="['/participant/accounts', a.id, 'beneficiaries']">Beneficiaries</a></p>

        <aside class="tape" aria-label="Contribution tape">
          <span>YTD EMPLOYEE {{ money(a.ytdEmployee) }}</span>
          <span>YTD MATCH {{ money(a.ytdMatch) }}</span>
          @if (a.deferralPercent !== null) {
            <span>NEXT PAYCHECK AT {{ pct(percent) }}</span>
          }
        </aside>
      }
    </section>
  `,
  styles: [`
    .page { padding: 2rem 2.5rem 6rem; max-width: 40rem; }
    a { color: var(--folder); }
    .eyebrow { letter-spacing: .16em; text-transform: uppercase; font-size: .72rem; color: var(--muted); }
    h1 { font-family: Sora, sans-serif; margin: .2rem 0; }
    .bal { font-family: "IBM Plex Mono", monospace; font-size: 2rem; margin: .2rem 0 1.5rem; }
    form { display: grid; gap: .7rem; max-width: 16rem; }
    input { border: 1px solid var(--grid); padding: .5rem .6rem; font-family: "IBM Plex Mono", monospace; }
    .primary { background: var(--folder); color: var(--folder-ink); border: 0; padding: .65rem 1rem; cursor: pointer; }
    .err { color: var(--ribbon); }
    .ok { color: var(--folder); }
    .note { color: var(--muted); }
    h2 { font-family: Sora, sans-serif; font-size: 1.05rem; margin: 1.6rem 0 .4rem; }
    .loan { display: grid; gap: .7rem; max-width: 22rem; }
    .tape {
      margin-top: 2rem; font-family: "IBM Plex Mono", monospace; font-size: .82rem;
      border-top: 1px dashed var(--grid); padding-top: .8rem; display: grid; gap: .3rem;
    }
  `]
})
export class AccountComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  account = signal<AccountSummary | null>(null);
  loans = signal<LoanRow[]>([]);
  percent = 0;
  loanAmount = 5000;
  error = signal<string | null>(null);
  loanError = signal<string | null>(null);
  money = money;
  pct = pct;

  pending(): LoanRow | undefined {
    return this.loans().find((row) => row.status === 'REQUESTED');
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<AccountSummary>(`${API_BASE}/accounts/${id}`).subscribe((a) => {
      this.account.set(a);
      this.percent = a.deferralPercent ?? 0;
    });
    this.refreshLoans();
  }

  save(): void {
    const a = this.account();
    if (!a) {
      return;
    }
    this.error.set(null);
    this.http.patch<AccountSummary>(`${API_BASE}/accounts/${a.id}/deferral`, { percent: this.percent })
      .subscribe({
        next: (updated) => {
          this.account.set(updated);
          this.percent = updated.deferralPercent ?? this.percent;
        },
        error: (err: HttpErrorResponse) => {
          const first = Array.isArray(err.error) ? err.error[0]?.message : null;
          this.error.set(first ?? 'Deferral was not saved.');
        }
      });
  }

  requestLoan(): void {
    const a = this.account();
    if (!a) {
      return;
    }
    this.loanError.set(null);
    this.http.post<LoanRow>(`${API_BASE}/accounts/${a.id}/loans`, { amount: this.loanAmount })
      .subscribe({
        next: () => this.refreshLoans(),
        error: (err: HttpErrorResponse) => {
          const first = Array.isArray(err.error) ? err.error[0]?.message : null;
          this.loanError.set(first ?? 'Loan was not requested.');
        }
      });
  }

  private refreshLoans(): void {
    this.http.get<LoanRow[]>(`${API_BASE}/loans/me`).subscribe((rows) => this.loans.set(rows));
  }
}
