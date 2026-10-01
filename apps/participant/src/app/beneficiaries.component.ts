import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { API_BASE, Beneficiary } from '@harborpath/shared';

@Component({
  selector: 'hp-beneficiaries',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="page">
      <a [routerLink]="['/participant/accounts', accountId]">Account</a>
      <h1>Beneficiaries</h1>
      <p class="lede">Percents must sum to 100. Saving replaces the designation on file. The 401(k) primary flag drives the sponsor compliance queue.</p>
      <table>
        <thead>
          <tr><th>Name</th><th>Relationship</th><th>%</th><th>Primary</th></tr>
        </thead>
        <tbody>
          @for (row of rows(); track $index) {
            <tr>
              <td><input [(ngModel)]="row.fullName" [name]="'n'+$index" /></td>
              <td><input [(ngModel)]="row.relationship" [name]="'r'+$index" /></td>
              <td><input type="number" [(ngModel)]="row.percent" [name]="'p'+$index" /></td>
              <td><input type="checkbox" [(ngModel)]="row.primaryFlag" [name]="'k'+$index" /></td>
            </tr>
          }
        </tbody>
      </table>
      @if (note()) {
        <p class="ok" role="status">{{ note() }}</p>
      }
      @if (error()) {
        <p class="err" role="alert">{{ error() }}</p>
      }
      <button class="primary" type="button" (click)="save()">Save designations</button>
    </section>
  `,
  styles: [`
    .page { padding: 2rem 2.5rem; max-width: 48rem; }
    a { color: var(--folder); }
    h1 { font-family: Sora, sans-serif; }
    table { width: 100%; border-collapse: collapse; margin: 1rem 0; }
    th { text-align: left; font-size: .8rem; border-bottom: 1px solid var(--grid); }
    td { padding: .4rem .2rem; }
    input { width: 100%; border: 1px solid var(--grid); padding: .35rem; }
    .primary { background: var(--folder); color: var(--folder-ink); border: 0; padding: .65rem 1rem; cursor: pointer; }
    .err { color: var(--ribbon); }
    .ok { color: var(--folder); }
  `]
})
export class BeneficiariesComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  accountId = '';
  rows = signal<Array<Pick<Beneficiary, 'fullName' | 'relationship' | 'percent' | 'primaryFlag'>>>([]);
  error = signal<string | null>(null);
  note = signal<string | null>(null);

  ngOnInit(): void {
    this.accountId = this.route.snapshot.paramMap.get('id') ?? '';
    this.http.get<Beneficiary[]>(`${API_BASE}/accounts/${this.accountId}/beneficiaries`)
      .subscribe((list) => this.rows.set(list.map(({ fullName, relationship, percent, primaryFlag }) =>
        ({ fullName, relationship, percent, primaryFlag }))));
  }

  save(): void {
    this.error.set(null);
    this.note.set(null);
    this.http.put<Beneficiary[]>(`${API_BASE}/accounts/${this.accountId}/beneficiaries`, this.rows())
      .subscribe({
        next: (list) => {
          this.rows.set(list.map(({ fullName, relationship, percent, primaryFlag }) =>
            ({ fullName, relationship, percent, primaryFlag })));
          const complete = list.some((row) => row.primaryFlag);
          this.note.set(complete
            ? 'Primary is on file. If a missing-primary item was open, it is now cleared.'
            : 'No primary on file. The sponsor compliance queue now has an open item.');
        },
        error: (err: HttpErrorResponse) => {
          const first = Array.isArray(err.error) ? err.error[0]?.message : null;
          this.error.set(first ?? 'Designations were not saved.');
        }
      });
  }
}
