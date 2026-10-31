import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';

const createToken = (claims: object): string => {
  const payload = btoa(JSON.stringify(claims))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${payload}.signature`;
};

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    window.sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('stores a login token and derives roles and permissions from its claims', () => {
    const token = createToken({
      username: 'sushil',
      exp: Math.floor(Date.now() / 1000) + 3600,
      role: [{ authority: 'ROLE_USER' }, { authority: 'PROFILE_READ' }]
    });

    auth.login({ email: 'sushil@example.com', password: 'Password1!' }).subscribe();
    const login = httpTesting.expectOne('/api/auth/login');
    expect(login.request.method).toBe('POST');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ username: 'sushil', jwt: token, expiresIn: 60 });

    expect(window.sessionStorage.getItem('access_token')).toBe(token);
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.currentUser()).toEqual({
      username: 'sushil',
      roles: ['ROLE_USER'],
      permissions: ['PROFILE_READ']
    });

    http.get('/api/profile').subscribe();
    const protectedRequest = httpTesting.expectOne('/api/profile');
    expect(protectedRequest.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    protectedRequest.flush({});
  });

  it('restores a valid session and clears an expired token', () => {
    window.sessionStorage.setItem('access_token', createToken({
      username: 'sushil',
      exp: Math.floor(Date.now() / 1000) + 3600,
      role: [{ authority: 'ROLE_ADMIN' }]
    }));

    auth.restoreSession();
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.hasRole('ROLE_ADMIN')).toBe(true);

    window.sessionStorage.setItem('access_token', createToken({
      username: 'sushil',
      exp: Math.floor(Date.now() / 1000) - 1
    }));
    auth.restoreSession();

    expect(auth.isAuthenticated()).toBe(false);
    expect(window.sessionStorage.getItem('access_token')).toBeNull();
  });
});
