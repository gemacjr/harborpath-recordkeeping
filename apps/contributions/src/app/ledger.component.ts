import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import {
  API_BASE,
  AuditRow,
  AuthSession,
  ContributionRow,
  HouseholdResponse,
  MeResponse,
  money,
  PayrollResult,
  PlanSummary
} from '@harborpath/shared';

@Component({
  selector: 'hp-ledger',
  imports: [FormsModule, DatePipe],
  template: `
    <section class="page">
      <p class="eyebrow">Contribution center</p>
      <h1>Ledger</h1>
      <p class="lede">
        Independently built remote. Payroll uses the 401(k) deferral from Household;
        529 cash and loan disbursements post here.
      </p>

      @if (role() === 'SPONSOR') {
        <form class="panel" (ngSubmit)="postPayroll()">
          <h2>Post a pay period</h2>
          <p>Employee deferral = period pay × current deferral %. Match is 50% of that.</p>
          <label>Period pay
            <input type="number" min="1" step="1" name="periodPay" [(ngModel)]="periodPay" />
          </label>
          @if (payrollNote()) {
            <p class="ok">{{ payrollNote() }}</p>
          }
          <button class="primary" type="submit">Post payroll</button>
        </form>
      }

      @if (role() === 'PARTICIPANT' && educationId) {
        <div class="split">
          <form class="panel" (ngSubmit)="contribute()">
            <h2>529 contribution</h2>
            <label>Amount
              <input type="number" min="1" step="0.01" name="contrib" [(ngModel)]="contribAmount" />
            </label>
            <button class="primary" type="submit">Post to Avery’s 529</button>
          </form>
          <form class="panel" (ngSubmit)="withdraw()">
            <h2>Qualified withdrawal</h2>
            <label>Amount
              <input type="number" min="1" step="0.01" name="wd" [(ngModel)]="withdrawAmount" />
            </label>
            <label>Memo
              <input name="memo" [(ngModel)]="withdrawMemo" />
            </label>
            <button class="primary" type="submit">Request withdrawal</button>
          </form>
        </div>
      }

      @if (error()) {
        <p class="err" role="alert">{{ error() }}</p>
      }

      <h2>Posted activity</h2>
      <table>
        <thead>
          <tr>
            <th>When</th>
            <th>Who</th>
            <th>Kind</th>
            <th>Account</th>
            <th>Amount</th>
            <th>Memo</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.id) {
            <tr>
              <td class="mono">{{ row.postedAt | date:'yyyy-MM-dd' }}</td>
              <td>{{ row.participantName }}</td>
              <td>{{ label(row.kind) }}</td>
              <td>{{ row.accountName }}</td>
              <td class="mono">{{ money(row.amount) }}</td>
              <td>{{ row.memo }}</td>
            </tr>
          }
        </tbody>
      </table>

      <h2>Mutation tape</h2>
      <p class="lede">Who changed what. Payroll and 529 cash still post above; this is the fiduciary trail.</p>
      <table>
        <thead>
          <tr>
            <th>When</th>
            <th>Actor</th>
            <th>Action</th>
            <th>Summary</th>
          </tr>
        </thead>
        <tbody>
          @for (row of audit(); track row.id) {
            <tr>
              <td class="mono">{{ row.occurredAt | date:'yyyy-MM-dd HH:mm' }}</td>
              <td>{{ row.actorUsername }}</td>
              <td>{{ auditLabel(row.action) }}</td>
              <td>{{ row.summary }}</td>
            </tr>
          }
        </tbody>
      </table>
    </section>
  `,
  styles: [`
    .page { padding: 2rem 2.5rem 4rem; max-width: 72rem; }
    .eyebrow { letter-spacing: .16em; text-transform: uppercase; font-size: .72rem; color: var(--muted); }
    h1 { font-family: Sora, sans-serif; margin: .2rem 0; }
    .lede, .err { color: var(--muted); }
    .err { color: var(--ribbon); }
    .ok { color: var(--folder); }
    h2 { font-family: Sora, sans-serif; font-size: 1.05rem; }
    .panel { background: var(--paper); border: 1px solid var(--grid); padding: 1rem; margin: 1rem 0; display: grid; gap: .6rem; max-width: 22rem; }
    .split { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    input { border: 1px solid var(--grid); padding: .45rem .55rem; font-family: "IBM Plex Mono", monospace; }
    .primary { background: var(--folder); color: var(--folder-ink); border: 0; padding: .55rem .9rem; cursor: pointer; width: fit-content; }
    table { width: 100%; border-collapse: collapse; }
    th { text-align: left; font-size: .75rem; text-transform: uppercase; border-bottom: 1px solid var(--grid); }
    td { padding: .55rem 0; border-bottom: 1px solid var(--grid); }
    .mono { font-family: "IBM Plex Mono", monospace; }
  `]
})
export class LedgerComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly session = inject(AuthSession);
  role = signal(this.session.role());
  rows = signal<ContributionRow[]>([]);
  audit = signal<AuditRow[]>([]);
  error = signal<string | null>(null);
  payrollNote = signal<string | null>(null);
  periodPay = 5000;
  contribAmount = 250;
  withdrawAmount = 100;
  withdrawMemo = 'Tuition';
  educationId: string | null = null;
  planId: string | null = null;
  money = money;

  ngOnInit(): void {
    this.http.get<MeResponse>(`${API_BASE}/me`).subscribe((me) => {
      this.role.set(me.role);
      if (me.role === 'PARTICIPANT') {
        this.http.get<HouseholdResponse>(`${API_BASE}/households/me`).subscribe((h) => {
          this.educationId = h.accounts.find((a) => a.type === 'EDUCATION_529')?.id ?? null;
        });
        this.loadMine();
      } else {
        this.http.get<PlanSummary[]>(`${API_BASE}/plans`).subscribe((plans) => {
          this.planId = plans[0]?.id ?? null;
          this.loadPlan();
        });
      }
    });
  }

  postPayroll(): void {
    if (!this.planId) {
      return;
    }
    this.error.set(null);
    this.http.post<PayrollResult>(`${API_BASE}/plans/${this.planId}/payroll`, { periodPay: this.periodPay })
      .subscribe({
        next: (res) => {
          this.payrollNote.set(
            `Posted ${res.accountsPosted} accounts · employee ${money(res.employeeTotal)} · match ${money(res.matchTotal)}`
          );
          this.loadPlan();
        },
        error: (err: HttpErrorResponse) => this.fail(err)
      });
  }

  contribute(): void {
    if (!this.educationId) {
      return;
    }
    this.error.set(null);
    this.http.post(`${API_BASE}/accounts/${this.educationId}/contributions`, { amount: this.contribAmount })
      .subscribe({ next: () => this.loadMine(), error: (err: HttpErrorResponse) => this.fail(err) });
  }

  withdraw(): void {
    if (!this.educationId) {
      return;
    }
    this.error.set(null);
    this.http.post(`${API_BASE}/accounts/${this.educationId}/withdrawals`, {
      amount: this.withdrawAmount,
      memo: this.withdrawMemo
    }).subscribe({ next: () => this.loadMine(), error: (err: HttpErrorResponse) => this.fail(err) });
  }

  label(kind: string): string {
    switch (kind) {
      case 'PAYROLL_EMPLOYEE':
        return 'Payroll deferral';
      case 'PAYROLL_MATCH':
        return 'Employer match';
      case 'EDUCATION_529':
        return '529 contribution';
      case 'QUALIFIED_WITHDRAWAL':
        return 'Qualified withdrawal';
      case 'LOAN_DISBURSEMENT':
        return '401(k) loan';
      default:
        return kind;
    }
  }

  auditLabel(action: string): string {
    switch (action) {
      case 'DEFERRAL_CHANGED':
        return 'Deferral';
      case 'BENEFICIARY_REPLACED':
        return 'Beneficiaries';
      case 'PAYROLL_POSTED':
        return 'Payroll';
      case 'EDUCATION_CONTRIBUTED':
        return '529 in';
      case 'EDUCATION_WITHDRAWN':
        return '529 out';
      case 'ALERT_ACKNOWLEDGED':
        return 'Alert ack';
      case 'ALERT_OPENED':
        return 'Alert opened';
      case 'ALERT_CLEARED':
        return 'Alert cleared';
      case 'LOAN_REQUESTED':
        return 'Loan request';
      case 'LOAN_APPROVED':
        return 'Loan disbursed';
      case 'LOAN_DENIED':
        return 'Loan denied';
      default:
        return action;
    }
  }

  private loadMine(): void {
    this.http.get<ContributionRow[]>(`${API_BASE}/contributions/me`).subscribe((rows) => this.rows.set(rows));
    this.http.get<AuditRow[]>(`${API_BASE}/audit/me`).subscribe((rows) => this.audit.set(rows));
  }

  private loadPlan(): void {
    if (!this.planId) {
      return;
    }
    this.http.get<ContributionRow[]>(`${API_BASE}/plans/${this.planId}/contributions`)
      .subscribe((rows) => this.rows.set(rows));
    this.http.get<AuditRow[]>(`${API_BASE}/plans/${this.planId}/audit`)
      .subscribe((rows) => this.audit.set(rows));
  }

  private fail(err: HttpErrorResponse): void {
    const first = Array.isArray(err.error) ? err.error[0]?.message : err.error?.error;
    this.error.set(first ?? 'Request was not posted.');
  }
}
