export type Role = 'PARTICIPANT' | 'SPONSOR';
export type AccountType = 'RETIREMENT_401K' | 'EDUCATION_529';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'CLEARED';

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  role: Role;
  displayName: string;
}

export interface MeResponse {
  id: string;
  username: string;
  displayName: string;
  role: Role;
}

export interface AccountSummary {
  id: string;
  type: AccountType;
  name: string;
  balance: number;
  deferralPercent: number | null;
  ytdEmployee: number;
  ytdMatch: number;
  beneficiaryComplete: boolean;
}

export interface HouseholdResponse {
  participantName: string;
  accounts: AccountSummary[];
  tape: { ytdEmployee: number; ytdMatch: number };
}

export interface Beneficiary {
  id: string;
  fullName: string;
  relationship: string;
  percent: number;
  primaryFlag: boolean;
}

export interface PlanSummary {
  id: string;
  name: string;
  planYear: number;
  participantCount: number;
  openAlertCount: number;
  deferralCapPercent: number;
}

export interface CensusRow {
  participantId: string;
  fullName: string;
  deferralPercent: number | null;
  balance: number;
  beneficiaryComplete: boolean;
}

export interface AlertRow {
  id: string;
  planId: string;
  participantId: string;
  code: string;
  message: string;
  severity: string;
  status: AlertStatus;
}

export interface ContributionRow {
  id: string;
  accountId: string;
  participantId: string;
  planId: string;
  participantName: string;
  kind: 'PAYROLL_EMPLOYEE' | 'PAYROLL_MATCH' | 'EDUCATION_529' | 'QUALIFIED_WITHDRAWAL' | 'LOAN_DISBURSEMENT';
  amount: number;
  postedAt: string;
  memo: string;
  accountName: string;
}

export interface PayrollResult {
  accountsPosted: number;
  employeeTotal: number;
  matchTotal: number;
}

export type AuditAction =
  | 'DEFERRAL_CHANGED'
  | 'BENEFICIARY_REPLACED'
  | 'PAYROLL_POSTED'
  | 'EDUCATION_CONTRIBUTED'
  | 'EDUCATION_WITHDRAWN'
  | 'ALERT_ACKNOWLEDGED'
  | 'ALERT_OPENED'
  | 'ALERT_CLEARED'
  | 'LOAN_REQUESTED'
  | 'LOAN_APPROVED'
  | 'LOAN_DENIED';

export interface AuditRow {
  id: string;
  occurredAt: string;
  actorUsername: string;
  actorRole: Role;
  planId: string;
  participantId: string | null;
  action: AuditAction;
  summary: string;
}

export interface FieldMessage {
  field: string;
  message: string;
}

export type LoanStatus = 'REQUESTED' | 'DENIED' | 'DISBURSED';

export interface LoanRow {
  id: string;
  accountId: string;
  participantId: string;
  planId: string;
  participantName: string;
  accountName: string;
  amount: number;
  status: LoanStatus;
  requestedAt: string;
  decidedAt: string | null;
}
