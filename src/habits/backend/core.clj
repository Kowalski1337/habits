(ns habits.backend.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes DELETE GET PATCH POST]]
            [compojure.route :as route]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [habits.backend.service.auth-service :as auth-service]
            [habits.backend.service.habits-service :as habits-service]
            [habits.backend.service.users-service :as users-service]))

(defroutes app-routes
           (GET    "/api/users"        req (users-service/get-all-users req))
           (GET    "/api/users/:id"    [id :as req] (users-service/get-user-by-id req id))

           (GET    "/api/habits"       req (habits-service/get-user-habits req))
           (POST   "/api/habits"       req (habits-service/create-habit! req))
           (PATCH  "/api/habits/:id"   [id :as req] (habits-service/update-habit! req id))
           (DELETE "/api/habits/:id"   [id :as req] (habits-service/delete-habit! req id))

           (POST   "/api/auth/login"   req (auth-service/login-or-register! req))

           (route/not-found {:error "Not found"}))

(defn wrap-base [handler]
  (-> handler
      wrap-json-response
      (wrap-json-body {:keywords? true})
      (wrap-cors :access-control-allow-origin  #".*"
                 :access-control-allow-methods [:get :post :put :delete :options :patch])))

(defn -main []
  (println "Starting server on port 3000...")
  (jetty/run-jetty (wrap-base app-routes) {:port 3000 :join? false}))