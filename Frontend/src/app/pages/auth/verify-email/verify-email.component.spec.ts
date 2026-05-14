import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { VerifyEmailComponent } from './verify-email.component';

describe('VerifyEmailComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  it('should show the invite-disabled state when invited=true is present', async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => (key === 'invited' ? 'true' : key === 'token' ? 'invite-token' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(VerifyEmailComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.state()).toBe('INVITE_DISABLED');
    httpMock.expectNone('http://localhost:8080/api/auth/verify-email?token=invite-token');
    fixture.destroy();
  });

  it('should call verify-email and move to success when the token is valid', async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => (key === 'token' ? 'valid-token' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(VerifyEmailComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/verify-email?token=valid-token');
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Email verified successfully.' });

    expect(fixture.componentInstance.state()).toBe('SUCCESS');
    fixture.destroy();
  });

  it('should show the expired state when the backend returns EXPIRED', async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => (key === 'token' ? 'expired-token' : null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(VerifyEmailComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/verify-email?token=expired-token');
    req.flush({ message: 'Token expired', errorType: 'EXPIRED' }, { status: 400, statusText: 'Bad Request' });

    expect(fixture.componentInstance.state()).toBe('ERROR_EXPIRED');
    fixture.destroy();
  });
});
