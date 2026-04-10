(ns habits.backend.service.service-utils
  (:require [habits.backend.response :as resp]))

(defn handle-result [entity-name result on-success]
  (if (:success result)
    (on-success (:data result))
    (case (:error-code result)
      :not-found (resp/not-found entity-name (get-in result [:details :id]))
      :database-error (resp/server-error (:message result))
      (resp/server-error "Unexpected error"))))

(defn parse-id [id-str]
  (try
    (Integer/parseInt id-str)
    (catch NumberFormatException _
      nil)))
