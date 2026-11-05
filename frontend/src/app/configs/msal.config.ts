import { Configuration } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

export const msalConfig: Configuration = {
  auth: {
    clientId: '2f281162-babf-4536-88c8-942f20273edb',
    authority: 'https://login.microsoftonline.com/2065748d-ab65-48bd-8110-f6e96d8fd02e',
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
    // El interceptor agrega el JWT a toda peticion cuya URL comience con este endpoint (el BFF)
    endpoint: environment.apiUrl,
    // endpoint: 'https://n9mfpgvsah.execute-api.us-east-1.amazonaws.com/api', // AWS (ya no se utiliza)
    scopes: ['api://2f281162-babf-4536-88c8-942f20273edb/AccesoUsuario'],
  },
};

export const loginRequest = {
  scopes: ['openid', 'profile', 'api://2f281162-babf-4536-88c8-942f20273edb/AccesoUsuario'],
};
