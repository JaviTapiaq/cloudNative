import { HttpInterceptorFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { from, Observable } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { protectedResources } from '../configs/msal.config';

export const msalInterceptor: HttpInterceptorFn = (req, next) => {
  const msalService = inject(MsalService);
  const account = msalService.instance.getActiveAccount();

  if (account && req.url.startsWith(protectedResources.bffApi.endpoint)) {
    const silentRequest = {
      account,
      scopes: protectedResources.bffApi.scopes,
    };

    return from(msalService.instance.acquireTokenSilent(silentRequest)).pipe(
      switchMap((response) => {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${response.accessToken}`,
          },
        });
        return next(authReq) as Observable<HttpEvent<unknown>>;
      })
    );
  }

  return next(req);
};
