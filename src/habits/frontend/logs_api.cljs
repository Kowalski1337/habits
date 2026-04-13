(ns habits.frontend.logs-api
  (:require [reagent.core :as r]
            [habits.frontend.auth :as auth]
            [habits.frontend.http :as http]
            [habits.frontend.config :as config]))

(defonce logs-state
         (r/atom {:logs     {}
                  :loading? false
                  :error    nil}))

(defn- user-id []
  (:id (:current-user @auth/auth-state)))

(defn- auth-headers []
  #js{"user-id" (str (user-id))})

(defn- auth-headers-json []
  #js{"Content-Type" "application/json"
      "user-id"      (str (user-id))})

(defn- handle-error! [err]
  (swap! logs-state assoc
         :error    (.-message err)
         :loading? false))

(defn fetch-logs! [year month]
  (swap! logs-state assoc :loading? true :error nil)
  (-> (js/fetch (str config/api-url "/api/habits/logs?year=" year "&month=" month)
                #js{:headers (auth-headers)})
      (.then http/check-response!)
      (.then (fn [data]
               (let [logs (js->clj data :keywordize-keys true)
                     by-date (group-by :date logs)]
                 (swap! logs-state assoc
                        :logs     by-date
                        :loading? false))))
      (.catch handle-error!)))

(defn upsert-log! [habit-id date completed emotion-color]
  (-> (js/fetch (str config/api-url "/api/habits/" habit-id "/logs")
                #js{:method  "POST"
                    :headers (auth-headers-json)
                    :body    (js/JSON.stringify
                               (clj->js {:date          date
                                         :completed     completed
                                         :emotion-color emotion-color}))})
      (.then http/check-response!)
      (.then (fn [log]
               (let [clean-log    (js->clj log :keywordize-keys true)
                     log-date     (:date clean-log)
                     log-habit-id (:habit-id clean-log)]
                 (swap! logs-state update-in [:logs log-date]
                        (fn [day-logs]
                          (let [existing (filterv #(not= (:habit-id %) log-habit-id)
                                                  (or day-logs []))]
                            (conj existing clean-log)))))))
      (.catch handle-error!)))