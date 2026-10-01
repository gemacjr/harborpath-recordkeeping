import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AlertRow, API_BASE, CensusRow, LoanRow, money, pct, PlanSummary } from '@harborpath/shared';

@Component({
  selector: 'hp-plan-book',
  template: `
    <section class="page">
      @if (plan(); as p) {
        <p class="eyebrow">Sponsor book</p>
        <h1>{{ p.name }}</h1>
        <p class="meta">Plan year {{ p.planYear }} · {{ p.participantCount }} participants · {{ p.openAlertCount }} open alerts</p>
      }

      <h2>Census</h2>
      @if (census().length === 0) {
        <p class="empty">No participants loaded for this plan year.</p>
      } @else {
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Deferral</th>
              <th>Balance</th>
              <th>Beneficiary</th>
            </tr>
          </thead>
          <tbody>
            @for (row of census(); track row.participantId) {
              <tr>
                <td>{{ row.fullName }}</td>
                <td class="mono">{{ pct(row.deferralPercent) }}</td>
                <td class="mono">{{ money(row.balance) }}</td>
                <td>{{ row.beneficiaryComplete ? 'On file' : 'Missing primary' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }

      <h2>Loan desk</h2>
      @if (loans().length === 0) {
        <p class="empty">No loan requests this plan year.</p>
      } @else {
        <ul class="queue">
          @for (loan of loans(); track loan.id) {
            <li>
              <span class="code">{{ loan.status }}</span>
              <p>{{ loan.participantName }} · {{ money(loan.amount) }} · {{ loan.accountName }}</p>
              @if (loan.status === 'REQUESTED') {
                <button type="button" (click)="decide(loan, 'APPROVE')">Approve &amp; disburse</button>
                <button type="button" class="ghost" (click)="decide(loan, 'DENY')">Deny</button>
              }
            </li>
          }
        </ul>
      }

      <h2>Compliance queue</h2>
      <ul class="queue">
        @for (alert of alerts(); track alert.id) {
          <li [class.cleared]="alert.status === 'CLEARED'">
            <span class="code">{{ alert.code }}</span>
            <p>{{ alert.message }}</p>
            <em>{{ statusLabel(alert.status) }}</em>
            @if (alert.status === 'OPEN') {
              <button type="button" (click)="acknowledge(alert)">Acknowledge</button>
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: [`
    .page { padding: 2rem 2.5rem 4rem; max-width: 72rem; }
    .eyebrow { letter-spacing: .16em; text-transform: uppercase; font-size: .72rem; color: var(--muted); }
    h1 { font-family: Sora, sans-serif; margin: .2rem 0; }
    .meta, .empty { color: var(--muted); }
    h2 { font-family: Sora, sans-serif; font-size: 1.1rem; margin-top: 2rem; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; font-size: .78rem; letter-spacing: .04em; text-transform: uppercase; border-bottom: 1px solid var(--grid); padding: .4rem 0; }
    td { padding: .65rem 0; border-bottom: 1px solid var(--grid); }
    .mono { font-family: "IBM Plex Mono", monospace; }
    .queue { list-style: none; padding: 0; display: grid; gap: .75rem; }
    .queue li { background: var(--paper); border: 1px solid var(--grid); padding: .9rem 1rem; }
    .queue li.cleared { opacity: .65; }
    .code { font-size: .72rem; background: var(--tab); padding: .1rem .4rem; }
    .queue button { background: var(--folder); color: var(--folder-ink); border: 0; padding: .4rem .7rem; cursor: pointer; margin-right: .4rem; }
    .queue button.ghost { background: transparent; color: inherit; border: 1px solid var(--grid); }
    em { font-style: normal; font-family: "IBM Plex Mono", monospace; font-size: .8rem; margin-right: .8rem; }
  `]
})
export class PlanBookComponent implements OnInit {
  private readonly http = inject(HttpClient);
  plan = signal<PlanSummary | null>(null);
  census = signal<CensusRow[]>([]);
  alerts = signal<AlertRow[]>([]);
  loans = signal<LoanRow[]>([]);
  money = money;
  pct = pct;

  statusLabel(status: AlertRow['status']): string {
    switch (status) {
      case 'OPEN':
        return 'OPEN';
      case 'ACKNOWLEDGED':
        return 'ACKNOWLEDGED';
      case 'CLEARED':
        return 'CLEARED by designations';
    }
  }

  ngOnInit(): void {
    this.http.get<PlanSummary[]>(`${API_BASE}/plans`).subscribe((plans) => {
      const first = plans[0];
      if (!first) {
        return;
      }
      this.plan.set(first);
      this.load(first.id);
    });
  }

  acknowledge(alert: AlertRow): void {
    this.http.patch<AlertRow>(`${API_BASE}/alerts/${alert.id}`, { status: 'ACKNOWLEDGED' })
      .subscribe(() => {
        const id = this.plan()?.id;
        if (id) {
          this.load(id);
        }
      });
  }

  decide(loan: LoanRow, decision: 'APPROVE' | 'DENY'): void {
    this.http.patch<LoanRow>(`${API_BASE}/loans/${loan.id}`, { decision })
      .subscribe(() => {
        const id = this.plan()?.id;
        if (id) {
          this.load(id);
        }
      });
  }

  private load(planId: string): void {
    this.http.get<CensusRow[]>(`${API_BASE}/plans/${planId}/census`).subscribe((rows) => this.census.set(rows));
    this.http.get<AlertRow[]>(`${API_BASE}/plans/${planId}/alerts`).subscribe((rows) => this.alerts.set(rows));
    this.http.get<LoanRow[]>(`${API_BASE}/plans/${planId}/loans`).subscribe((rows) => this.loans.set(rows));
    this.http.get<PlanSummary[]>(`${API_BASE}/plans`).subscribe((plans) => this.plan.set(plans[0] ?? null));
  }
}
