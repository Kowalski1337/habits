(ns habits.backend.service.habit-logs-service
  (:require [clojure.string :as str]
            [habits.backend.dao.habit-logs-dao :as dao]
            [habits.backend.response :as resp]
            [habits.backend.service.service-utils :refer [handle-result parse-id]]))

(defn- get-user-id [req]
  (let [user-id (parse-id (get-in req [:headers "user-id"]))]
    (if (nil? user-id)
      (resp/bad-request "Invalid or missing user-id" {:header "user-id"})
      user-id)))

(defn get-logs-for-month [req]
  (let [user-id (get-user-id req)
        year (parse-id (get-in req [:query-params "year"]))
        month (parse-id (get-in req [:query-params "month"]))]
    (cond
      (resp/error? user-id)
      user-id

      (nil? year)
      (resp/bad-request "Invalid or missing year" {:param "year"})

      (nil? month)
      (resp/bad-request "Invalid or missing month" {:param "month"})

      (not (<= 1 month 12))
      (resp/bad-request "Month must be between 1 and 12" {:param "month"})

      :else
      (handle-result "Habit log" (dao/get-logs-for-month user-id year month)
                     resp/success))))

(defn upsert-log! [req habit-id]
  (let [user-id (get-user-id req)
        habit-id-int (parse-id habit-id)
        {:keys [date completed emotion-color]} (:body req)]
    (cond
      (resp/error? user-id)
      user-id

      (nil? habit-id-int)
      (resp/bad-request "Invalid habit-id" {:habit-id habit-id})

      (str/blank? date)
      (resp/bad-request "Date is required" {:field "date"})

      (nil? completed)
      (resp/bad-request "Completed is required" {:field "completed"})

      :else
      (handle-result
        "Habit log"
        (dao/upsert-log! user-id habit-id-int date completed (or emotion-color "#000000"))
        resp/success))))