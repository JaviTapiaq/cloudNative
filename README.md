# Pedidos360 - Cloud Native I

Sistema de gestion de pedidos con arquitectura cloud native: tienda en Angular con login de
Azure (Entra ID), BFF que valida el JWT, microservicios Spring Boot, base de datos en Supabase
y mensajeria asincrona con RabbitMQ (colas, exchanges y DLQs).

## Estructura del Proyecto

```
CLOUDNATIVE_Ev1/
├── backend/
│   ├── bff/                # Backend For Frontend, valida el JWT (puerto 8080)
│   ├── ms-clientes/        # Clientes + consumidor de compras (puerto 8081)
│   ├── ms-productos/       # Catalogo + consumidor de stock (puerto 8082)
│   ├── ms-pedidos/         # Pedidos, productor de eventos (puerto 8083)
│   ├── ms-mensajeria/      # Notificaciones, tickets, documentos y admin de RabbitMQ (puerto 8084)
│   ├── docker-compose.yml  # Levanta todo el sistema
│   └── .env.example        # Plantilla de variables (Supabase, roles)
└── frontend/               # Angular 19 + MSAL (puerto 4200)
```

## Tecnologias

- **Frontend**: Angular 19, MSAL (@azure/msal-angular), Microsoft Entra ID
- **Backend**: Java 17, Spring Boot 3.4, Spring AMQP
- **BFF**: Spring Security OAuth2 Resource Server + filtro JWT propio
- **Base de datos**: Supabase (PostgreSQL) o H2 en memoria para desarrollo
- **Mensajeria**: RabbitMQ 3.13
- **Contenedores**: Docker + Docker Compose

## Como ejecutar (Docker, recomendado)

Requiere Docker Desktop en ejecucion.

```powershell
cd backend
copy .env.example .env      # solo la primera vez; editar si se usa Supabase
docker compose up -d --build
```

| Componente | URL |
|---|---|
| Aplicacion | http://localhost:4200 |
| BFF | http://localhost:8080 |
| Consola RabbitMQ | http://localhost:15672 (`pedidos360` / `pedidos360`) |

Comandos utiles:

```powershell
docker compose ps                      # estado de los contenedores
docker compose logs -f ms-mensajeria   # logs de un servicio (ACK, NACK, DLQ)
docker compose stop ms-clientes        # detener un servicio (demo de desacoplamiento)
docker compose down                    # detener todo
```

### Base de datos en Supabase

Por defecto se usa H2 en memoria. Para usar Supabase, completar en `backend/.env`:

```
SPRING_PROFILES_ACTIVE=supabase
SUPABASE_DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres
SUPABASE_DB_USER=postgres.<id-del-proyecto>
SUPABASE_DB_PASSWORD=<clave>
```

Cada microservicio crea sus tablas en su propio schema (`productos`, `pedidos`, `clientes`).
`ms-productos` carga 6 productos de ejemplo si su tabla esta vacia.

## Como ejecutar (sin Docker)

Cada microservicio usa Maven Wrapper (no requiere Maven instalado). RabbitMQ debe estar
corriendo (`docker compose up -d rabbitmq`).

```powershell
cd backend\ms-clientes;   .\mvnw.cmd spring-boot:run   # 8081
cd backend\ms-productos;  .\mvnw.cmd spring-boot:run   # 8082
cd backend\ms-pedidos;    .\mvnw.cmd spring-boot:run   # 8083
cd backend\ms-mensajeria; .\mvnw.cmd spring-boot:run   # 8084
cd backend\bff;           .\mvnw.cmd spring-boot:run   # 8080
cd frontend; npm install; npm start                    # 4200
```

## Seguridad (Azure Entra ID)

1. El frontend inicia sesion con MSAL y agrega `Authorization: Bearer <JWT>` a cada llamada al BFF.
2. El BFF valida el token con dos filtros:
   - `BearerTokenAuthenticationFilter` (Spring Security): firma, issuer, audience y expiracion (401).
   - `JwtAuthFilter` (propio): roles, identidad del usuario y log de accesos (403).
3. Los microservicios solo se exponen al BFF, que les reenvia la identidad del usuario.

Con `BFF_ENFORCE_ROLES=true` el BFF exige los roles de aplicacion `Admin`/`User` asignados en
Azure (Aplicaciones empresariales > Usuarios y grupos) y el rol `Admin` para el monitor.

## Endpoints

Todas las rutas se consumen a traves del BFF (`http://localhost:8080/api/...`).

### ms-productos
- `GET    /api/productos` - Listar todos
- `GET    /api/productos/{id}` - Obtener por ID
- `GET    /api/productos/categoria/{categoria}` - Filtrar por categoria
- `POST   /api/productos` / `PUT /api/productos/{id}` / `DELETE /api/productos/{id}`

### ms-pedidos
- `GET    /api/pedidos` - Listar todos
- `GET    /api/pedidos/mios` - Pedidos del usuario autenticado
- `GET    /api/pedidos/{id}` - Obtener por ID
- `POST   /api/pedidos` - Crear pedido (valida precio y stock en ms-productos; publica `pedido.creado`)
- `PATCH  /api/pedidos/{id}/estado` - Cambiar estado (publica `pedido.estado.actualizado`)
- `DELETE /api/pedidos/{id}` - Eliminar

### ms-clientes
- `GET    /api/clientes` - Listar todos (incluye historial de compras)
- `GET    /api/clientes/{id}` / `POST` / `PUT` / `DELETE`

### ms-mensajeria (administracion de RabbitMQ)
- `GET    /api/admin/topologia` - Exchanges, colas y DLQs configuradas
- `GET    /api/admin/queues` - Estado de todas las colas del sistema
- `POST   /api/admin/queues` - Crear cola (opcional: TTL, largo maximo, DLQ, binding)
- `GET    /api/admin/queues/{name}` - Estado de una cola
- `DELETE /api/admin/queues/{name}` - Eliminar cola (`?soloSiVacia=true&incluirDlq=true`)
- `DELETE /api/admin/queues/{name}/messages` - Purgar cola
- `POST   /api/admin/exchanges` / `DELETE /api/admin/exchanges/{name}` - Crear / eliminar exchange
- `POST   /api/admin/bindings` / `DELETE /api/admin/bindings` - Crear / eliminar binding
- `POST   /api/mensajeria/{notificaciones|tickets|documentos}` - Encolar tarea (202)
- `GET    /api/mensajeria/registro` - Bitacora de procesamiento (ACK, reintentos, DLQ)

## H2 Console (perfil default)

- ms-clientes: http://localhost:8081/h2-console
- ms-productos: http://localhost:8082/h2-console
- ms-pedidos: http://localhost:8083/h2-console

JDBC URL: `jdbc:h2:mem:<db-name>`, User: `sa`, Password: (vacio)
