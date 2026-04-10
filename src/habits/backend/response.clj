(ns habits.backend.response)

(def error-statuses
  {:bad-request  400
   :unauthorized 401
   :forbidden    403
   :not-found    404
   :conflict     409
   :server-error 500})

(defn success [data]
  {:status 200
   :body   data})

(defn created [data]
  {:status 201
   :body   data})

(defn no-content []
  {:status 204
   :body   nil})

(defn error
  ([status message] (error status message nil))
  ([status message details]
   (cond-> {:status (get error-statuses status 500)
            :body   (cond-> {:error   (name status)
                             :message message}
                            details (assoc :details details))})))

(defn bad-request
  ([message] (error :bad-request message))
  ([message details] (error :bad-request message details)))

(defn unauthorized
  ([] (unauthorized "Unauthorized"))
  ([message] (error :unauthorized message)))

(defn server-error
  ([] (server-error "Internal server error"))
  ([message] (error :server-error message)))

(defn not-found [entity id]
  (error :not-found (str entity " with id " id " not found") {:entity entity :id id}))

(defn error?
  [response]
  (and (map? response) (:status response) (>= (:status response) 400)))