(ns habits.backend.service.habits-service
  (:require [clojure.string :as str]
            [habits.backend.dao.habits-dao :as dao]
            [habits.backend.response :as resp]
            [habits.backend.service.service-utils :refer [handle-result parse-id]]))


(defn- get-user-id [req]
  (let [user-id-str (get-in req [:headers "user-id"])
        user-id (parse-id user-id-str)]
    (if (nil? user-id)
      (resp/bad-request "Invalid or missing user-id" {:header "user-id"})
      user-id)))


(defn create-habit! [req]
  (let [user-id (get-user-id req)
        {:keys [title description color]} (:body req)]
    (cond
      (resp/error? user-id)
      user-id

      (or (nil? title) (str/blank? title))
      (resp/bad-request "Title is required" {:field "title"})

      :else
      (handle-result "Habit" (dao/create-habit! user-id title description color)
                           resp/created))))

(defn get-user-habits [req]
  (let [user-id (get-user-id req)]
    (if (resp/error? user-id)
      user-id
      (handle-result "Habit" (dao/get-user-habits user-id) resp/success))))

(defn update-habit! [req habit-id]
  (let [user-id (get-user-id req)
        habit-id-int (parse-id habit-id)
        {:keys       [title description color]
         order-index :order-index} (:body req)]
    (cond
      (resp/error? user-id)
      user-id

      (nil? habit-id-int)
      (resp/bad-request "Invalid habit-id" {:habit-id habit-id})

      (every? nil? [title description color order-index])
      (resp/bad-request "At least one field must be provided for update"
                        {:allowed-fields [:title :description :color :order-index]})

      :else
      (handle-result "Habit" (dao/update-habit! habit-id-int title description color order-index)
                           resp/success))))

(defn delete-habit! [req habit-id]
  (let [user-id (get-user-id req)
        habit-id-int (parse-id habit-id)]
    (cond
      (resp/error? user-id)
      user-id

      (nil? habit-id-int)
      (resp/bad-request "Invalid habit-id" {:habit-id habit-id})

      :else
      (handle-result "Habit" (dao/delete-habit! habit-id-int)
                           (fn [_] (resp/no-content))))))