(ns habits.backend.response)

(def error-statuses
  {:bad-request    400
   :unauthorized   401
   :forbidden      403
   :not-found      404
   :conflict       409
   :server-error   500})

(defn success [data]
  {:status 200
   :body data})

(defn created [data]
  {:status 201
   :body data})

(defn no-content []
  {:status 204
   :body nil})

(defn error
  ([status message]
   {:status (get error-statuses status 500)
    :body {:error (name status)
           :message message}})
  ([status message details]
   {:status (get error-statuses status 500)
    :body {:error (name status)
           :message message
           :details details}}))

(defn bad-request [message & [details]]
  (error :bad-request message details))

(defn not-found [entity id]
  (error :not-found (str entity " with id " id " not found") {:entity entity :id id}))

(defn unauthorized [& [message]]
  (error :unauthorized (or message "Unauthorized")))

(defn server-error [& [message]]
  (error :server-error (or message "Internal server error")))

(defn is-error?
  [response]
  (and (map? response) (:status response) (>= (:status response) 400)))