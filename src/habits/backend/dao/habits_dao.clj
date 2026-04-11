(ns habits.backend.dao.habits-dao
  (:require [habits.backend.dao.dao-utils :refer [error success with-db-error]]
            [habits.backend.db :as db]))

(defn create-habit!
  [user-id title description]
  (with-db-error "Failed to create habit"
    (success (db/get-one
               "INSERT INTO habits (user_id, title, description)
                   VALUES (?, ?, ?)
                   RETURNING id, title, description, created_at"
               user-id title description))))

(defn get-user-habits
  [user-id]
  (with-db-error "Failed to fetch habits"
    (success (db/execute!
               "SELECT id, title, description, created_at
                    FROM habits
                    WHERE user_id = ?
                    ORDER BY created_at"
               user-id))))

(defn update-habit!
  [habit-id title description]
  (with-db-error "Failed to update habit"
    (success (db/get-one
               "UPDATE habits
                    SET title = COALESCE(?, title),
                        description = COALESCE(?, description),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    RETURNING id, title, description, user_id, created_at"
               title description habit-id))))

(defn delete-habit!
  [habit-id]
  (with-db-error "Failed to delete habit"
    (let [rows-affected (db/execute! "DELETE FROM habits WHERE id = ?" habit-id)]
      (if (empty? rows-affected)
        (error :not-found "Habit not found" {:id habit-id})
        (success {:id habit-id :deleted true})))))