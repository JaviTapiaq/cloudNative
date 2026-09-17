import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { MSAL_INSTANCE, MSAL_GUARD_CONFIG, MSAL_INTERCEPTOR_CONFIG, MsalService, MsalGuard, MsalInterceptor, MsalBroadcastService } from '@azure/msal-angular';
import { PublicClientApplication, InteractionType } from '@azure/msal-browser';
import { msalConfig, protectedResources, loginRequest } from './configs/msal.config';
import { routes } from './app.routes';
import { msalInterceptor } from './interceptors/msal.interceptor';

function MSALInstanceFactory(): PublicClientApplication {
  return new PublicClientApplication(msalConfig);
}

function MSALGuardConfigFactory() {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: loginRequest,
  };
}

function MSALInterceptorConfigFactory() {
  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap: new Map([
      [protectedResources.bffApi.endpoint, protectedResources.bffApi.scopes],
    ]),
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([msalInterceptor])),
    {
      provide: MSAL_INSTANCE,
      useFactory: MSALInstanceFactory,
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: MSALGuardConfigFactory,
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: MSALInterceptorConfigFactory,
    },
    MsalService,
    MsalGuard,
    MsalInterceptor,
    MsalBroadcastService,
  ],
};
