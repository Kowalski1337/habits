(ns habits.backend.dao.habits-dao
  (:require [habits.backend.db :as db]
            [habits.backend.dao.dao-response :refer [success, error]]))

(defn create-habit!
  [user-id title description color]
  (try
    (let [result (db/execute!
                   "INSERT INTO habits (user_id, title, description, color)
                   VALUES (?, ?, ?, ?)
                   RETURNING id, title, description, color, created_at, order_index"
                   user-id title description color)
          habit (first result)]
      (success habit))
    (catch Exception e
      (error :database-error "Failed to create habit" {:cause (.getMessage e)}))))

(defn get-user-habits
  [user-id]
  (try
    (let [habits (db/execute!
                   "SELECT id, title, description, color, created_at, order_index
                    FROM habits
                    WHERE user_id = ?
                    ORDER BY order_index, created_at"
                   user-id)]
      (success habits))
    (catch Exception e
      (error :database-error "Failed to fetch habits" {:cause (.getMessage e)}))))

(defn update-habit!
  [habit-id title description color order-index]
  (try
    (let [result (db/execute!
                   "UPDATE habits
                    SET title = COALESCE(?, title),
                        description = COALESCE(?, description),
                        color = COALESCE(?, color),
                        order_index = COALESCE(?, order_index),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    RETURNING id, title, description, color, order_index, user_id, created_at"
                   title description color order-index habit-id)
          updated-habit (first result)]
      (success updated-habit))
    (catch Exception e
      (error :database-error "Failed to update habit" {:cause (.getMessage e)}))))

(defn delete-habit!
  [habit-id]
  (try
    (let [rows-affected (db/execute! "DELETE FROM habits WHERE id = ?" habit-id)]
      (if (zero? (count rows-affected))
        (error :not-found "Habit not found" {:habit-id habit-id})
        (success {:id habit-id :deleted true})))
    (catch Exception e
      (error :database-error "Failed to delete habit" {:cause (.getMessage e)}))))