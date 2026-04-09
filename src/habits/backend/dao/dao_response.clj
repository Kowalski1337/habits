(ns habits.backend.dao.dao-response)

(defn success [data]
  {:success true :data data})

(defn error [error-code message & [details]]
  {:success    false
   :error-code error-code
   :message    message
   :details    details})
