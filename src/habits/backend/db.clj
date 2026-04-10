(ns habits.backend.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import (com.zaxxer.hikari HikariConfig HikariDataSource)))

;; TODO use environ
(defn env
  ([key]
   (System/getenv key))
  ([key default]
   (or (System/getenv key) default)))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   (env "DB_NAME" "habits_dev")
   :host     (env "DB_HOST" "localhost")
   :port     (Integer/parseInt (env "DB_PORT" "5432"))
   :user     (env "DB_USER" "postgres")
   :password (env "DB_PASSWORD" "postgres")})

(def jdbc-opts {:builder-fn rs/as-unqualified-maps})

(defn make-datasource []
  (HikariDataSource.
    (doto (HikariConfig.)
      (.setJdbcUrl (str "jdbc:postgresql://" (:host db-spec) ":" (:port db-spec) "/" (:dbname db-spec)))
      (.setUsername (:user db-spec))
      (.setPassword (:password db-spec))
      (.setMaximumPoolSize 10)
      (.setMinimumIdle 2)
      (.setConnectionTimeout 30000)
      (.setIdleTimeout 600000))))

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
    (jdbc/execute! @datasource (into [sql] params) jdbc-opts)
    (catch Exception e
      (println "Database error:" (.getMessage e))
      (throw e))))

(defn get-one
  [sql & params]
  (first (apply execute! sql params)))