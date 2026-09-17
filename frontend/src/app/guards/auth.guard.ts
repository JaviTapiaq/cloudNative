import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { filter, map, take } from 'rxjs/operators';

export const authGuard: CanActivateFn = (route, state) => {
  const msalService = inject(MsalService);
  const msalBroadcastService = inject(MsalBroadcastService);
  const router = inject(Router);

  return msalBroadcastService.inProgress$.pipe(
    filter((status: InteractionStatus) => status === InteractionStatus.None),
    take(1),
    map(() => {
      const account = msalService.instance.getActiveAccount();
      if (account) {
        return true;
      }
      router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    })
  );
};
