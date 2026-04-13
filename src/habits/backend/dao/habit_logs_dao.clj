(ns habits.backend.dao.habit-logs-dao
  (:require [habits.backend.dao.dao-utils :refer [error success with-db-error]]
            [habits.backend.db :as db]))

(defn get-logs-for-month [user-id year month]
  (with-db-error "Failed to fetch habit logs"
    (success (db/execute!
               "SELECT hl.id, hl.habit_id, hl.date, hl.completed, hl.emotion_color
                FROM habit_logs hl
                JOIN habits h ON h.id = hl.habit_id
                WHERE h.user_id = ?
                  AND EXTRACT(YEAR FROM hl.date) = ?
                  AND EXTRACT(MONTH FROM hl.date) = ?
                ORDER BY hl.date"
               user-id year month))))

(defn upsert-log! [user-id habit-id date completed emotion-color]
  (with-db-error "Failed to upsert habit log"
    (let [result (db/execute!
                   "INSERT INTO habit_logs (habit_id, date, completed, emotion_color)
                    SELECT ?, ?, ?, ?
                    FROM habits
                    WHERE id = ? AND user_id = ?
                    ON CONFLICT (habit_id, date)
                    DO UPDATE SET completed     = EXCLUDED.completed,
                                  emotion_color = EXCLUDED.emotion_color,
                                  updated_at    = CURRENT_TIMESTAMP
                    RETURNING id, habit_id, date, completed, emotion_color"
                   habit-id date completed emotion-color habit-id user-id)]
      (if (empty? result)
        (error :not-found "Habit not found or access denied" {:habit-id habit-id})
        (success (first result))))))