/**
 * Configuracion central del frontend. Todas las llamadas al backend pasan por el BFF.
 */
export const environment = {
  apiUrl: 'http://localhost:8080/api',
  // Despliegue anterior en AWS API Gateway (ya no se utiliza):
  // apiUrl: 'https://n9mfpgvsah.execute-api.us-east-1.amazonaws.com/api',
};
