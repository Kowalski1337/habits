(ns habits.backend.db
  (:require [next.jdbc :as jdbc]
            [com.zaxxer.hikari :as hikari]
            [environ.core :refer [env]]))

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
   :user (env "DB_USER")
   :password (env "DB_PASSWORD" )})

(defonce datasource
         (delay
           (println "Creating database connection pool...")
           (println (str "Connecting to: " (:dbname db-spec) "@" (:host db-spec) ":" (:port db-spec)))
           (try
             (hikari/make-datasource db-spec)
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