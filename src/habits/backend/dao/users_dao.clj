(ns habits.backend.dao.users-dao
  (:require [habits.backend.db :as db]
            [habits.backend.dao.dao-response :refer [success, error]]))

(defn get-all-users
  []
  (try
    (let [users (db/execute!
                  "SELECT id, name, created_at FROM users ORDER BY id")]
      (success users))
    (catch Exception e
      (error :database-error "Failed to fetch users" {:cause (.getMessage e)}))))

(defn get-user-by-id
  [user-id]
  (try
    (let [user (db/get-one "SELECT id, name, created_at FROM users WHERE id = ?" user-id)]
      (if user
        (success user)
        (error :not-found "User not found" {:user-id user-id})))
    (catch Exception e
      (error :database-error "Failed to fetch user" {:cause (.getMessage e)}))))

(defn find-user-by-name
  [name]
  (try
    (let [res (db/execute!
                 "SELECT id, name, password_hash, created_at
                  FROM users
                  WHERE name = ?"
                 name)
          user (first res)]
      (if user
        (success user)
        (error :not-found "User not found" {:user-name name})))
    (catch Exception e
      (error :database-error "Failed to fetch user" {:cause (.getMessage e)}))))

(defn create-user!
  [name password-hash]
  (try
    (let [result (db/execute!
                   "INSERT INTO users (name, password_hash) VALUES (?, ?) RETURNING id"
                   name, password-hash)
          user-id (-> result first :users/id)]
      (success {:id user-id :name name}))
    (catch Exception e
      (error :database-error "Failed to create user" {:cause (.getMessage e)}))))