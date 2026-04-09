(ns habits.backend.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.core :refer [defroutes GET POST PATCH DELETE]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.cors :refer [wrap-cors]]
            [habits.backend.service.users-service :as users-service]
            [habits.backend.service.auth-service :as auth-service]
            [habits.backend.service.habits-service :as habits-service]))

(defroutes app-routes
           (GET "/api/health" [] {:status 200 :body {:status "ok"}})

           (GET "/api/users" [] users-service/get-all-users)
           (GET "/api/users/:id" [id] #(users-service/get-user-by-id % id))
           (POST "/api/users" [] users-service/create-user!)

           (GET "/api/habits" [] habits-service/get-user-habits)
           (POST "/api/habits" [] habits-service/create-habit!)
           (PATCH "/api/habits/:id" [id] #(habits-service/update-habit! % id))
           (DELETE "/api/habits/:id" [id] habits-service/delete-habit! id)

           (POST "/api/auth/login" [] auth-service/login-or-register!)

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