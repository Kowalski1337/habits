(ns habits.backend.service.habits-service
  (:require [habits.backend.dao.habits-dao :as dao]
            [habits.backend.response :as resp]))

(defn parse-id [id-str]
  (try
    (Integer/parseInt id-str)
    (catch NumberFormatException _
      nil)))

(defn get-user-id [req]
  (let [user-id-str (get-in req [:headers "user-id"])
        user-id (parse-id user-id-str)]
    (when (nil? user-id)
      (resp/bad-request "Invalid or missing user-id" {:header "user-id"}))
    user-id))

(defn create-habit!
  [req]
  (let [user-id (get-user-id req)
        body (:body req)
        title (:title body)
        description (:description body)
        color (:color body)]

    (cond
      (resp/is-error? user-id)
      user-id

      (or (nil? title) (clojure.string/blank? title))
      (resp/bad-request "Title is required" {:field "title"})

      :else
      (let [result (dao/create-habit! user-id title description color)]
        (if (:success result)
          (resp/created (:data result))
          (case (:error-code result)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))

(defn get-user-habits
  [req]
  (let [user-id (get-user-id req)]
    (if (resp/is-error? user-id)
      user-id
      (let [result (dao/get-user-habits user-id)]
        (if (:success result)
          (resp/success (:data result))
          (resp/server-error (:message result)))))))

(defn update-habit!
  [req habit-id]
  (let [user-id (get-user-id req)
        habit-id-int (parse-id habit-id)
        body (:body req)
        title (:title body)
        description (:description body)
        color (:color body)
        order-index (:order_index body)]

    (cond
      (resp/is-error? user-id)
      user-id

      (nil? habit-id-int)
      (resp/bad-request "Invalid habit-id" {:habit-id habit-id})

      (and (nil? title) (nil? description) (nil? color) (nil? order-index))
      (resp/bad-request "At least one field must be provided for update"
                        {:allowed-fields [:title :description :color :order_index]})

      :else
      (let [result (dao/update-habit! habit-id-int title description color order-index)]
        (if (:success result)
          (resp/success (:data result))
          (case (:error-code result)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))

(defn delete-habit!
  [req habit-id]
  (let [user-id (get-user-id req)
        habit-id-int (parse-id habit-id)]
    (cond
      (resp/is-error? user-id) user-id
      (nil? habit-id-int) (resp/bad-request "Invalid habit-id" {:habit-id habit-id})
      :else
      (let [result (dao/delete-habit! habit-id-int)]
        (if (:success result)
          (resp/no-content)
          (case (:error-code result)
            :not-found (resp/not-found "Habit" habit-id-int)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))