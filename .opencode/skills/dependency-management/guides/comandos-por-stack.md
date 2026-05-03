# comandos-por-stack

Resumen ejecutable de comandos por stack. Esta tabla es la fuente de
verdad cuando un agente necesita ejecutar build/test/install:

### react-nextjs / vue-nuxt / astro-hono / react-nestjs / react-native-expo
- Verifica: `node -v && npm -v` (≥20 / ≥10)
- Manifest: `package.json`
- Install: `npm ci` (o `pnpm install --frozen-lockfile`)
- Build: `npm run build`
- Run: `npm run dev`

### angular-springboot / angular-quarkus
- Verifica: `java -version && mvn -version` (Java 21+, Maven 3.8+) y `node -v` para el frontend
- Manifest: `backend/pom.xml` y `frontend/package.json`
- Install backend: `cd backend && mvn install -DskipTests`
- Install frontend: `cd frontend && npm ci`
- Run: `mvn spring-boot:run` (o `mvn quarkus:dev`) y `npm run start`

### python-fastapi / python-django
- Verifica: `python3 --version` (3.12+) y `pip3 --version`
- Manifest: `requirements.txt` o `pyproject.toml`
- Install: `python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt`
- Run FastAPI: `uvicorn app.main:app --reload`
- Run Django: `python manage.py runserver`

### go-fiber
- Verifica: `go version` (1.22+)
- Manifest: `go.mod`
- Install: `go mod download`
- Build: `go build ./...`
- Run: `go run .`

### rust-axum
- Verifica: `rustc --version && cargo --version` (1.80+)
- Manifest: `Cargo.toml`
- Install: `cargo fetch`
- Build: `cargo build`
- Run: `cargo run`

### dotnet-blazor
- Verifica: `dotnet --version` (8.0+)
- Manifest: `*.csproj` y `*.sln`
- Install: `dotnet restore`
- Build: `dotnet build`
- Run: `dotnet run`

### flutter-dart
- Verifica: `flutter --version && dart --version` (Flutter 3.24+)
- Manifest: `pubspec.yaml`
- Install: `flutter pub get`
- Run: `flutter run`
