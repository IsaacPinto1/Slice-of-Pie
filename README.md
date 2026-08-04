## Running the Project Locally

### 1. Start the Database

Start the local database using Docker:

```bash
docker compose up -d
```

To stop the database and remove all persisted data:

```bash
docker compose down -v
```

---

### 2. Start the Backend

Run the Spring Boot backend:

```bash
./mvnw spring-boot:run
```

Or run it with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### 3. Start the Frontend

Run the React development server:

```bash
npm run dev
```

The frontend should now be available at the URL shown in your terminal (typically `http://localhost:5173` for Vite).