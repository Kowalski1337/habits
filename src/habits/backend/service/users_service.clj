(ns habits.backend.service.users-service
  (:require [habits.backend.dao.users-dao :as dao]
            [habits.backend.response :as resp]))

(defn parse-id [id-str]
  (try
    (Integer/parseInt id-str)
    (catch NumberFormatException _
      nil)))

(defn get-all-users
  [_]
  (let [result (dao/get-all-users)]
    (if (:success result)
      (resp/success (:data result))
      (case (:error-code result)
        :database-error (resp/server-error (:message result))
        (resp/server-error "Unexpected error")))))

(defn get-user-by-id
  [_ user-id]
  (let [user-id-int (parse-id user-id)]
    (if (nil? user-id-int)
      (resp/bad-request "Invalid user-id" {:user-id user-id})
      (let [result (dao/get-user-by-id user-id-int)]
        (if (:success result)
          (resp/success (:data result))
          (case (:error-code result)
            :not-found (resp/not-found "User" user-id-int)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))

(defn create-user!
  [req]
  (let [body (:body req)
        name (:name body)]
    (if (or (nil? name) (clojure.string/blank? name))
      (resp/bad-request "Name is required" {:field "name"})
      (let [result (dao/create-user! name)]
        (if (:success result)
          (resp/created (:data result))
          (case (:error-code result)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))