(ns habits.backend.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]
            [habits.backend.db :as db]))

(defroutes app-routes
           (GET "/api/health" [] {:status 200 :body {:status "ok"}})
           (GET "/api/test" [] {:status 200 :body {:message "Hello from Clojure!"}})
           (GET "/api/users" []
             (try
               (let [users (db/execute! "SELECT id, name, created_at FROM users ORDER BY id")]
                 {:status 200 :body {:users users}})
               (catch Exception e
                 {:status 500 :body {:error "Failed to fetch users" :details (.getMessage e)}})))
           (route/not-found {:status 404 :body {:error "Not found"}}))

(defn wrap-base [handler]
  (-> handler
      wrap-json-response
      (wrap-json-body {:keywords? true})
      (wrap-cors :access-control-allow-origin #".*"
                 :access-control-allow-methods [:get :post :put :delete :options])))

(defn -main []
  (println "Starting server on port 3000...")
  (jetty/run-jetty (wrap-base app-routes) {:port 3000 :join? false}))