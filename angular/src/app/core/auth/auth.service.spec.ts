import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';

const createToken = (claims: object): string => {
  const payload = btoa(JSON.stringify(claims))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${payload}.signature`;
};

const tokenResponse = (username = 'sushil', role = 'ROLE_USER') => ({
  username,
  jwt: createToken({
    username,
    exp: Math.floor(Date.now() / 1000) + 3600,
    role: [{ authority: role }]
  }),
  expiresIn: 60
});

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: { url: '', navigate: () => Promise.resolve(true) } },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('keeps the login token in memory and derives roles from its claims', () => {
    const response = tokenResponse();

    auth.login({ email: 'sushil@example.com', password: 'Password1!' }).subscribe();
    const login = httpTesting.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush(response);

    expect(auth.accessToken()).toBe(response.jwt);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.currentUser()).toEqual({
      username: 'sushil',
      roles: ['ROLE_USER'],
      permissions: []
    });

    http.get('/api/profile').subscribe();
    const protectedRequest = httpTesting.expectOne('/api/profile');
    expect(protectedRequest.request.headers.get('Authorization')).toBe(`Bearer ${response.jwt}`);
    protectedRequest.flush({});
  });

  it('restores a session through the HttpOnly refresh cookie', () => {
    let restored = false;
    auth.restoreSession().subscribe(() => {
      restored = true;
    });

    const refresh = httpTesting.expectOne('/api/auth/refresh');
    expect(refresh.request.method).toBe('POST');
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush(tokenResponse('admin', 'ROLE_ADMIN'));

    expect(restored).toBe(true);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.hasRole('ROLE_ADMIN')).toBe(true);
  });

  it('treats a failed session restore as logged out without erroring', () => {
    let restored = false;
    let errored = false;
    auth.restoreSession().subscribe({
      next: () => {
        restored = true;
      },
      error: () => {
        errored = true;
      }
    });

    httpTesting.expectOne('/api/auth/refresh').flush(
      { message: 'Invalid refresh token' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(restored).toBe(true);
    expect(errored).toBe(false);
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('refreshes once and retries parallel unauthorized requests with the new token', () => {
    const first = tokenResponse();
    auth.login({ email: 'sushil@example.com', password: 'Password1!' }).subscribe();
    httpTesting.expectOne('/api/auth/login').flush(first);

    let successes = 0;
    for (let index = 0; index < 5; index++) {
      http.get(`/api/profile?call=${index}`).subscribe(() => successes++);
    }

    const initialRequests = httpTesting.match((request) => request.url.startsWith('/api/profile'));
    expect(initialRequests).toHaveLength(5);
    initialRequests.forEach((request) =>
      request.flush({}, { status: 401, statusText: 'Unauthorized' })
    );

    const refreshed = tokenResponse('sushil', 'ROLE_ADMIN');
    const refreshRequests = httpTesting.match('/api/auth/refresh');
    expect(refreshRequests).toHaveLength(1);
    expect(refreshRequests[0].request.withCredentials).toBe(true);
    refreshRequests[0].flush(refreshed);

    const retries = httpTesting.match((request) => request.url.startsWith('/api/profile'));
    expect(retries).toHaveLength(5);
    retries.forEach((request) => {
      expect(request.request.headers.get('Authorization')).toBe(`Bearer ${refreshed.jwt}`);
      request.flush({});
    });

    expect(successes).toBe(5);
    expect(auth.accessToken()).toBe(refreshed.jwt);
  });

  it('does not refresh or add bearer authorization to authentication endpoints', () => {
    auth.login({ email: 'sushil@example.com', password: 'wrong' }).subscribe({
      error: () => undefined
    });

    const login = httpTesting.expectOne('/api/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush(
      { message: 'Invalid email or password' },
      { status: 401, statusText: 'Unauthorized' }
    );

    httpTesting.expectNone('/api/auth/refresh');
    httpTesting.expectNone('/api/auth/logout');
  });

  it('calls the logout endpoint before clearing the in-memory session', () => {
    const response = tokenResponse();
    auth.login({ email: 'sushil@example.com', password: 'Password1!' }).subscribe();
    httpTesting.expectOne('/api/auth/login').flush(response);

    auth.logout().subscribe();
    const logout = httpTesting.expectOne('/api/auth/logout');
    expect(logout.request.method).toBe('POST');
    expect(logout.request.withCredentials).toBe(true);
    expect(auth.isAuthenticated()).toBe(true);
    logout.flush(null);

    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.currentUser()).toBeNull();
  });
});
