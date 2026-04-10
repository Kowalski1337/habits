(ns habits.backend.dao.habits-dao
  (:require [habits.backend.dao.dao-utils :refer [error success with-db-error]]
            [habits.backend.db :as db]))

(defn create-habit!
  [user-id title description color]
  (with-db-error "Failed to create habit"
    (success (db/get-one
               "INSERT INTO habits (user_id, title, description, color)
                   VALUES (?, ?, ?, ?)
                   RETURNING id, title, description, color, created_at, order_index"
               user-id title description color))))

(defn get-user-habits
  [user-id]
  (with-db-error "Failed to fetch habits"
    (success (db/execute!
               "SELECT id, title, description, color, created_at, order_index
                    FROM habits
                    WHERE user_id = ?
                    ORDER BY order_index, created_at"
               user-id))))

(defn update-habit!
  [habit-id title description color order-index]
  (with-db-error "Failed to update habit"
    (success (db/get-one
               "UPDATE habits
                    SET title = COALESCE(?, title),
                        description = COALESCE(?, description),
                        color = COALESCE(?, color),
                        order_index = COALESCE(?, order_index),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    RETURNING id, title, description, color, order_index, user_id, created_at"
               title description color order-index habit-id))))

(defn delete-habit!
  [habit-id]
  (with-db-error "Failed to delete habit"
    (let [rows-affected (db/execute! "DELETE FROM habits WHERE id = ?" habit-id)]
      (if (empty? rows-affected)
        (error :not-found "Habit not found" {:habit-id habit-id})
        (success {:id habit-id :deleted true})))))