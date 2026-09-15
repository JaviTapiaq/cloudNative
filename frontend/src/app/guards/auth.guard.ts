import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

export const authGuard: CanActivateFn = (route, state) => {
  const msalService = inject(MsalService);
  const router = inject(Router);

  const account = msalService.instance.getActiveAccount();

  if (account) {
    return true;
  }

  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
