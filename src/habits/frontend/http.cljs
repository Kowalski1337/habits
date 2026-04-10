(ns habits.frontend.http)

(defn throw-error-response! [response]
  (-> (.json response)
      (.then (fn [data]
               (throw (js/Error. (.-message data)))))))

(defn check-response! [response]
  (if (.-ok response)
    (.json response)
    (throw-error-response! response)))