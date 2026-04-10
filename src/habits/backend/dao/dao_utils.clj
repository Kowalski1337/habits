(ns habits.backend.dao.dao-utils)

(defn success [data]
  {:success true :data data})

(defn error
  ([error-code message] (error error-code message nil))
  ([error-code message details]
   (cond-> {:success    false
            :error-code error-code
            :message    message}
           details (assoc :details details))))

(defmacro with-db-error [error-msg & body]
  `(try
     ~@body
     (catch Exception e#
       (error :database-error ~error-msg {:cause (.getMessage e#)}))))
