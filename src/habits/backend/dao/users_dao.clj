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

(defn create-user!
  [name]
  (try
    (let [result (db/execute!
                   "INSERT INTO users (name) VALUES (?) RETURNING id"
                   name)
          user-id (-> result first :users/id)]
      (success {:id user-id :name name}))
    (catch Exception e
      (error :database-error "Failed to create user" {:cause (.getMessage e)}))))