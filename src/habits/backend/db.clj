(ns habits.backend.db
  (:require [next.jdbc :as jdbc]
            [environ.core :refer [env]])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

(defn env
  ([key]
   (System/getenv key))
  ([key default]
   (or (System/getenv key) default)))

(def db-spec
  {:dbtype "postgresql"
   :dbname (env "DB_NAME" "habits_dev")
   :host (env "DB_HOST" "localhost")
   :port (Integer/parseInt (env "DB_PORT" "5432"))
   :user (env "DB_USER" "postgres")
   :password (env "DB_PASSWORD" "postgres")})

(defn make-datasource []
  (let [config (HikariConfig.)]
    (.setJdbcUrl config (str "jdbc:postgresql://" (:host db-spec) ":" (:port db-spec) "/" (:dbname db-spec)))
    (.setUsername config (:user db-spec))
    (.setPassword config (:password db-spec))
    (.setMaximumPoolSize config 10)
    (.setMinimumIdle config 2)
    (.setConnectionTimeout config 30000)
    (.setIdleTimeout config 600000)
    (HikariDataSource. config)))

(defonce datasource
         (delay
           (println "Creating database connection pool...")
           (println (str "Connecting to: " (:dbname db-spec) "@" (:host db-spec) ":" (:port db-spec)))
           (try
             (make-datasource)
             (catch Exception e
               (println "Failed to create datasource:" (.getMessage e))
               (throw e)))))

(defn execute!
  [sql & params]
  (try
    (jdbc/execute! @datasource (into [sql] params))
    (catch Exception e
      (println "Database error:" (.getMessage e))
      (throw e))))