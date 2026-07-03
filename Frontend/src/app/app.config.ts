import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { TitleStrategy, provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { tokenRefreshInterceptor } from './core/interceptors/token-refresh.interceptor';
import { AxiomTitleStrategy } from './core/title/axiom-title.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection(),
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor, tokenRefreshInterceptor])),
    { provide: TitleStrategy, useClass: AxiomTitleStrategy }
  ]
};
