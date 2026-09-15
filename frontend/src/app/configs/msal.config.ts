import { Configuration } from '@azure/msal-browser';

export const msalConfig: Configuration = {
  auth: {
    // PLACEHOLDER: Reemplazar con el clientId de la aplicacion registrada en Azure AD
    clientId: '',
    // PLACEHOLDER: Reemplazar con el tenant ID de Azure AD
    authority: 'https://login.microsoftonline.com/',
    redirectUri: 'http://localhost:4200/',
    postLogoutRedirectUri: 'http://localhost:4200/login',
  },
  cache: {
    cacheLocation: 'localStorage',
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message) => {
        console.log(`[MSAL] ${level}: ${message}`);
      },
      logLevel: 2,
    },
  },
};

export const protectedResources = {
  bffApi: {
    endpoint: 'http://localhost:8080/api',
    // PLACEHOLDER: Reemplazar con el scope de la API expuesta en Azure AD
    // Formato: api://<api-client-id>/<scope-name>
    scopes: [''],
  },
};

export const loginRequest = {
  scopes: ['openid', 'profile'],
};
