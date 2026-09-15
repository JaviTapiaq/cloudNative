# Pedidos360 - Cloud Native I

Sistema de gestion de pedidos con arquitectura cloud native.

## Estructura del Proyecto

```
CLOUDNATIVE_Ev1/
├── backend/
│   ├── ms-clientes/      # Microservicio de Clientes (puerto 8081)
│   ├── ms-productos/     # Microservicio de Productos (puerto 8082)
│   ├── ms-pedidos/       # Microservicio de Pedidos (puerto 8083)
│   └── bff/              # Backend For Frontend (puerto 8080)
└── frontend/             # Angular 19 + MSAL (puerto 4200)
```

## Tecnologias

- **Frontend**: Angular 19, MSAL (@azure/msal-angular), Azure AD
- **Backend**: Java 17, Spring Boot 3.4, H2 (en memoria)
- **BFF**: Spring Boot 3.4, Nimbus JOSE JWT para validacion de tokens

## Como ejecutar

### Backend - Microservicios

Cada microservicio usa Maven Wrapper (no requiere Maven instalado).

```bash
# ms-clientes (puerto 8081)
cd backend/ms-clientes
./mvnw spring-boot:run

# ms-productos (puerto 8082)
cd backend/ms-productos
./mvnw spring-boot:run

# ms-pedidos (puerto 8083)
cd backend/ms-pedidos
./mvnw spring-boot:run
```

### Backend - BFF

```bash
cd backend/bff
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Abrir http://localhost:4200

## Configuracion pendiente (PLACEHOLDERS)

### Azure AD (IDaaS)

1. **Frontend** (`src/app/configs/msal.config.ts`):
   - `clientId`: Client ID de la app registrada en Azure AD
   - `authority`: URL del tenant (`https://login.microsoftonline.com/<tenant-id>`)
   - `scopes`: Scope de la API expuesta (`api://<api-client-id>/<scope>`)

2. **BFF** (`src/main/resources/application.properties`):
   - `bff.idaas.issuer`: Issuer del tenant
   - `bff.idaas.audience`: Audience (clientId de la API)
   - `bff.idaas.jwks-uri`: URL de las llaves publicas del tenant

## Endpoints de los microservicios

### ms-clientes
- `GET    /api/clientes` - Listar todos
- `GET    /api/clientes/{id}` - Obtener por ID
- `POST   /api/clientes` - Crear
- `PUT    /api/clientes/{id}` - Actualizar
- `DELETE /api/clientes/{id}` - Eliminar

### ms-productos
- `GET    /api/productos` - Listar todos
- `GET    /api/productos/{id}` - Obtener por ID
- `GET    /api/productos/categoria/{categoria}` - Filtrar por categoria
- `POST   /api/productos` - Crear
- `PUT    /api/productos/{id}` - Actualizar
- `DELETE /api/productos/{id}` - Eliminar

### ms-pedidos
- `GET    /api/pedidos` - Listar todos
- `GET    /api/pedidos/{id}` - Obtener por ID
- `GET    /api/pedidos/cliente/{clienteId}` - Pedidos de un cliente
- `POST   /api/pedidos` - Crear pedido
- `PATCH  /api/pedidos/{id}/estado` - Cambiar estado
- `DELETE /api/pedidos/{id}` - Eliminar

### BFF
- `/{servicio}/**` - Proxy a los microservicios con validacion JWT
  - `/api/clientes/**` -> ms-clientes
  - `/api/productos/**` -> ms-productos
  - `/api/pedidos/**` -> ms-pedidos

## H2 Console

Cada microservicio expone la consola H2 en `/h2-console`:
- ms-clientes: http://localhost:8081/h2-console
- ms-productos: http://localhost:8082/h2-console
- ms-pedidos: http://localhost:8083/h2-console

JDBC URL: `jdbc:h2:mem:<db-name>`, User: `sa`, Password: (vacio)
