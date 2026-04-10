(ns habits.backend.dao.users-dao
  (:require [habits.backend.dao.dao-utils :refer [error success with-db-error]]
            [habits.backend.db :as db]))

(defn get-all-users []
  (with-db-error "Failed to fetch users"
    (success (db/execute! "SELECT id, name, created_at FROM users ORDER BY id"))))

(defn get-user-by-id [user-id]
  (with-db-error "Failed to fetch user"
    (if-let [user (db/get-one "SELECT id, name, created_at FROM users WHERE id = ?" user-id)]
      (success user)
      (error :not-found "User not found" {:id user-id}))))

(defn find-user-by-name [name]
  (with-db-error "Failed to fetch user"
    (if-let [user (db/get-one "SELECT id, name, password_hash, created_at FROM users WHERE name = ?" name)]
      (success user)
      (error :not-found "User not found" {:user-name name}))))

(defn create-user! [name password-hash]
  (with-db-error "Failed to create user"
    (success (db/get-one "INSERT INTO users (name, password_hash) VALUES (?, ?) RETURNING id, name"
                         name password-hash))))