import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('should send a PUT request for changePassword with confirmPassword', () => {
    const payload = {
      currentPassword: 'Current123',
      newPassword: 'NewPassword123',
      confirmPassword: 'NewPassword123'
    };

    service.changePassword(payload).subscribe((response) => {
      expect(response.message).toBe('Password updated');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/change-password');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);

    req.flush({ message: 'Password updated' });
  });

  it('should clear stored auth data and navigate to login when logging out without a token', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    sessionStorage.setItem('adr_refresh_token', 'refresh-token');
    sessionStorage.setItem('adr_user', JSON.stringify({ id: 'user-1' }));

    service.logout();

    expect(sessionStorage.getItem('adr_token')).toBeNull();
    expect(sessionStorage.getItem('adr_refresh_token')).toBeNull();
    expect(sessionStorage.getItem('adr_user')).toBeNull();
    expect(navigateSpy).toHaveBeenCalledWith('/login');
  });

  it('should treat expired tokens as unauthenticated', () => {
    sessionStorage.setItem('adr_token', btoa(encodeURIComponent(createJwt({ exp: Math.floor(Date.now() / 1000) - 60 }))));

    expect(service.isAuthenticated()).toBe(false);
  });
});

function createJwt(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>): string =>
    btoa(JSON.stringify(value))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/g, '');

  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.signature`;
}
