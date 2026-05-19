# Khelder Web — Backend + Frontend

Sistema de monitorización de salud para cuidadores. Este repositorio contiene el backend Spring Boot y el frontend React del proyecto Khelder, un TFG de ingeniería informática orientado a la telemonitorización de pacientes mediante smartwatch.

---

## Estructura del repositorio

```
khelder-web/
├── backend/                        # API REST + WebSocket (Spring Boot)
│   ├── src/main/java/com/khelder/backend/
│   │   ├── config/                 # Configuración Spring (Security, WebSocket, Firebase, CORS)
│   │   ├── controller/             # Controladores REST
│   │   ├── dto/                    # Data Transfer Objects (request/response)
│   │   │   ├── auth/               # DTOs de autenticación y pairing
│   │   │   ├── biometric/          # DTOs de registros biométricos
│   │   │   └── websocket/          # DTOs de notificaciones WebSocket
│   │   ├── entity/                 # Entidades JPA
│   │   ├── repository/             # Repositorios Spring Data JPA
│   │   ├── security/               # JWT filter, UserDetailsService
│   │   └── service/                # Lógica de negocio
│   ├── src/main/resources/
│   │   ├── application.yml         # Configuración Spring Boot
│   │   ├── data.sql                # Datos iniciales (cuidadores y pacientes)
│   │   └── init-db.sql             # Extensiones PostgreSQL (PostGIS, uuid-ossp)
│   ├── Dockerfile                  # Imagen Docker del backend
│   └── pom.xml                     # Dependencias Maven
├── frontend/                       # SPA React
│   ├── src/
│   │   ├── api/                    # Clientes Axios por dominio
│   │   ├── components/             # Componentes reutilizables
│   │   │   ├── alerts/             # AlertItem
│   │   │   ├── biometric/          # BiometricHistory, HeartRateChart
│   │   │   ├── dashboard/          # PatientMap, DeviceStatus, ConnectionStatus
│   │   │   ├── layout/             # Layout, Navbar
│   │   │   └── ui/                 # Button, Input
│   │   ├── context/                # AuthContext
│   │   ├── hooks/                  # useAuth, useWebSocket
│   │   ├── pages/                  # Páginas de la aplicación
│   │   └── types/                  # Tipos TypeScript globales
│   ├── Dockerfile                  # Imagen Docker con Nginx
│   ├── nginx.conf                  # Proxy inverso al backend
│   └── package.json
└── docker-compose.yml              # Orquestación completa
```

---

## Stack tecnológico

### Backend
| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.5.14 | Framework REST + WebSocket |
| Spring Security | 6.x | Autenticación JWT |
| PostgreSQL | 16 | Base de datos principal |
| PostGIS | 3.4 | Extensión geoespacial para zonas seguras |
| JTS (locationtech) | — | Geometría espacial en Java |
| Firebase Admin SDK | 9.3.0 | Envío de notificaciones push (FCM) |
| dotenv-java | 3.0.0 | Carga de variables de entorno |
| iTextPDF | — | Generación de informes PDF |
| Apache Commons CSV | — | Generación de informes CSV |
| Maven | 3.9.6 | Gestión de dependencias |

### Frontend
| Tecnología | Versión | Uso |
|---|---|---|
| React | 18 | Framework UI |
| TypeScript | 5.x | Tipado estático |
| Vite | 5.x | Bundler y servidor de desarrollo |
| Tailwind CSS | 3.x | Estilos utilitarios |
| Recharts | — | Gráficas de frecuencia cardíaca y SpO2 |
| Leaflet + React-Leaflet | — | Mapas interactivos y zonas seguras |
| STOMP.js + SockJS | — | WebSocket en tiempo real |
| Axios + axios-case-converter | — | Cliente HTTP con conversión snake_case/camelCase |
| React Router | 6.x | Enrutamiento SPA |
| Lucide React | — | Iconografía |

---

## Requisitos previos

- Docker y Docker Compose
- (Desarrollo local) JDK 21, Node.js 20, PostgreSQL 16 con extensión PostGIS

---

## Variables de entorno

Crea el archivo `backend/.env` con las siguientes variables:

```dotenv
# PostgreSQL
POSTGRES_DB=khelder_db
POSTGRES_USER=khelder_user
POSTGRES_PASSWORD=tu_password_segura
POSTGRES_PORT=5433

# Spring Boot
SPRING_PORT=8080

# JWT
JWT_SECRET=tu_secreto_jwt_minimo_32_caracteres
JWT_EXPIRATION=86400000

# Firebase Cloud Messaging
FIREBASE_CREDENTIALS_BASE64=base64_del_json_de_firebase
```

Copia el archivo para Docker Compose:
```bash
cp backend/.env .env
```

---

## Despliegue con Docker

```bash
# Construir e iniciar todos los servicios
docker compose up --build -d

# Ver estado de los contenedores
docker compose ps

# Ver logs del backend
docker compose logs backend -f
```

Los servicios estarán disponibles en:
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5433

---

## Desarrollo local

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

El frontend estará disponible en http://localhost:5173.

---

## API REST — Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/login` | Autenticación JWT |
| POST | `/api/v1/auth/register` | Registro de cuidador |
| GET | `/api/v1/patients` | Listar pacientes del cuidador |
| POST | `/api/v1/patients` | Crear paciente |
| DELETE | `/api/v1/patients/{id}` | Eliminar paciente y sus datos |
| POST | `/api/v1/biometric-records` | Guardar registro biométrico |
| GET | `/api/v1/alerts` | Listar alertas activas |
| PUT | `/api/v1/alerts/{id}/resolve` | Resolver alerta |
| GET | `/api/v1/safe-zones/patient/{id}` | Zonas seguras de un paciente |
| POST | `/api/v1/reminders` | Crear recordatorio |
| POST | `/api/v1/pairing/register` | Registrar código de vinculación |
| GET | `/api/v1/pairing/status/{code}` | Consultar estado del pairing |
| POST | `/api/v1/pairing/fcm-token` | Registrar token FCM del móvil |
| GET | `/api/v1/reports` | Generar informe PDF/CSV |

### WebSocket STOMP
- `/topic/alerts/{caregiverId}` — Alertas en tiempo real
- `/topic/vitals/{caregiverId}` — Datos biométricos en tiempo real
- `/topic/location/{caregiverId}` — Ubicación GPS en tiempo real

---

## Credenciales de prueba

```
carlos@khelder.com / password123
ana@khelder.com / password123
```

---

## Tests

```bash
cd backend
./mvnw test
```

49 tests activos. 8 deshabilitados por incompatibilidad de H2 con PostGIS.

---

## Despliegue en producción (Digital Ocean)

El proyecto está desplegado en un Droplet Ubuntu 22.04 con Docker Compose. Para actualizar:

```bash
ssh root@IP_DROPLET
cd ~/app/web
git pull
docker compose up --build -d
```
