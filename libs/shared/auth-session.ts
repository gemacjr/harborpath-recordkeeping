import { Injectable, signal } from '@angular/core';
import { LoginResponse, Role } from './models';

const KEY = 'harborpath.session';

interface StoredSession {
  accessToken: string;
  role: Role;
  displayName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthSession {
  readonly token = signal<string | null>(null);
  readonly role = signal<Role | null>(null);
  readonly displayName = signal<string | null>(null);
  readonly apiDown = signal(false);

  constructor() {
    const raw = localStorage.getItem(KEY);
    if (!raw) {
      return;
    }
    try {
      const parsed = JSON.parse(raw) as StoredSession;
      this.token.set(parsed.accessToken);
      this.role.set(parsed.role);
      this.displayName.set(parsed.displayName);
    } catch {
      localStorage.removeItem(KEY);
    }
  }

  set(login: LoginResponse): void {
    const stored: StoredSession = {
      accessToken: login.accessToken,
      role: login.role,
      displayName: login.displayName
    };
    localStorage.setItem(KEY, JSON.stringify(stored));
    this.token.set(login.accessToken);
    this.role.set(login.role);
    this.displayName.set(login.displayName);
    this.apiDown.set(false);
  }

  clear(): void {
    localStorage.removeItem(KEY);
    this.token.set(null);
    this.role.set(null);
    this.displayName.set(null);
  }

  homePath(): string {
    return this.role() === 'SPONSOR' ? '/sponsor' : '/participant';
  }
}
