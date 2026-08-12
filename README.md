# Spring Boot CRUD Project

A complete Spring Boot backend application with REST API for user management.

## Features

- ✅ Spring Boot 3.1.5 with Java 17
- ✅ Spring Data JPA for database operations
- ✅ RESTful API endpoints
- ✅ Input validation with Jakarta Validation
- ✅ Global exception handling
- ✅ H2 in-memory database for development
- ✅ MySQL support for production
- ✅ Lombok for reduced boilerplate code
- ✅ CORS support

## Project Structure

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── Application.java          # Main application entry point
│   │   ├── controller/               # REST endpoints
│   │   │   └── UserController.java
│   │   ├── service/                  # Business logic
│   │   │   └── UserService.java
│   │   ├── repository/               # Data access layer
│   │   │   └── UserRepository.java
│   │   ├── model/                    # Entity classes
│   │   │   └── User.java
│   │   └── exception/                # Exception handling
│   │       └── GlobalExceptionHandler.java
│   └── resources/
│       ├── application.properties     # Default configuration
│       ├── application-dev.properties # Development profile
│       └── application-prod.properties # Production profile
└── test/
    └── java/                          # Test classes
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 5.7+ (for production)

## Setup Instructions

### 1. Clone the repository
```bash
git clone <repository-url>
cd Spring-Crud-Project
```

### 2. Install dependencies
```bash
mvn clean install
```

### 3. Run the application

**Development (H2 Database):**
```bash
mvn spring-boot:run
```

Or run with specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Production (MySQL):**
Before running with MySQL:
1. Update `application-prod.properties` with your database credentials
2. Create the database: `CREATE DATABASE crud_db;`
3. Run the application:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

## API Endpoints

All endpoints are prefixed with `/api/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all users |
| GET | `/{id}` | Get user by ID |
| GET | `/email/{email}` | Get user by email |
| POST | `/` | Create new user |
| PUT | `/{id}` | Update user |
| DELETE | `/{id}` | Delete user |

### Example Requests

**Create User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","phone":"1234567890","address":"123 Main St"}'
```

**Get All Users:**
```bash
curl http://localhost:8080/api/users
```

**Get User by ID:**
```bash
curl http://localhost:8080/api/users/1
```

**Update User:**
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Doe"}'
```

**Delete User:**
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## Database Profiles

### Development (H2)
- **URL:** `jdbc:h2:mem:testdb`
- **Console:** `http://localhost:8080/h2-console`
- **Username:** `sa`
- **Password:** (empty)

### Production (MySQL)
- Update credentials in `application-prod.properties`
- Database name: `crud_db`

## Configuration

### Hibernate DDL Auto Options
- `create` - Create schema on startup, drop on shutdown
- `create-drop` - Create schema on startup, drop on shutdown
- `update` - Update schema on startup (recommended for development)
- `validate` - Validate schema on startup (recommended for production)

## Building

### Development Build
```bash
mvn clean package
```

### Production Build
```bash
mvn clean package -Dmaven.test.skip=true
```

Run the JAR:
```bash
java -jar target/spring-crud-project-1.0.0.jar --spring.profiles.active=prod
```

## Testing

Run tests with:
```bash
mvn test
```

## Dependencies

- **spring-boot-starter-web** - Web and RESTful API
- **spring-boot-starter-data-jpa** - Database operations
- **spring-boot-starter-validation** - Input validation
- **lombok** - Reduce boilerplate
- **h2** - In-memory database (dev)
- **mysql-connector-j** - MySQL driver
- **spring-boot-starter-test** - Testing framework

## Adding New Entities

1. Create entity class in `src/main/java/com/example/model/`
2. Create repository interface extending `JpaRepository`
3. Create service class with business logic
4. Create controller class with REST endpoints
5. Add validation annotations as needed

## Error Handling

The application includes global exception handling for:
- Validation errors (400 Bad Request)
- Duplicate email errors (400 Bad Request)
- Not found errors (404 Not Found)
- Server errors (500 Internal Server Error)

## Logging

Configure logging levels in `application.properties`:
```properties
logging.level.root=INFO
logging.level.com.example=DEBUG
```

## License

This project is open source and available under the MIT License.

## Support

For issues or questions, please create an issue in the repository.
