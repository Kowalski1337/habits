(ns habits.backend.service.users-service
  (:require [habits.backend.dao.users-dao :as dao]
            [habits.backend.entity.user-entity :as entity]
            [habits.backend.response :as resp]))

(defn parse-id [id-str]
  (try
    (Integer/parseInt id-str)
    (catch NumberFormatException _
      nil)))

(defn get-all-users
  [_]
  (let [result (dao/get-all-users)]
    (println result)
    (if (:success result)
      (resp/success (->> result :data (map entity/build)))
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
          (resp/success (:data (entity/build result)))
          (case (:error-code result)
            :not-found (resp/not-found "User" user-id-int)
            :database-error (resp/server-error (:message result))
            (resp/server-error "Unexpected error")))))))
