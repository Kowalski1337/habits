(ns habits.backend.service.users-service
  (:require [habits.backend.dao.users-dao :as dao]
            [habits.backend.response :as resp]
            [habits.backend.service.service-utils :refer [handle-result parse-id]]))

(defn get-all-users [_]
  (handle-result "User" (dao/get-all-users) resp/success))

(defn get-user-by-id [_ user-id]
  (if-let [user-id-int (parse-id user-id)]
    (handle-result "User" (dao/get-user-by-id user-id-int) resp/success)
    (resp/bad-request "Invalid user-id" {:user-id user-id})))