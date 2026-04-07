# habits

A Clojure web-application designed to track daily habits.

## Usage

Setup env vars for database
Copy .env-template file to .env and fill secret data

Create PostgreSQL
```bash
docker-compose up -d
```

Connect to db inside docker container
```bash
docker exec -it habits-postgres psql -U {{DB_USER}} -d {{DB_NAME}}
```

Create new migration
```bash
lein migratus create {{NAME}}
```
Then edit created files `DATE-name.up.sql` (apply) and `DATE-name.down.sql` (rollback)

Apply migrations
```bash
lein migratus migrate  
```

Run backend
```bash
lein run
```
Run frontend
```bash
npx shadow-cljs watch frontend
```

## Technologies

- **Backend**: Clojure, Ring, Compojure, next.jdbc, PostgreSQL
- **Frontend**: ClojureScript, Reagent, shadow-cljs, TailwindCSS
- **Build**: Leiningen + shadow-cljs + npm

