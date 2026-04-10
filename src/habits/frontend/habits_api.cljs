(ns habits.frontend.habits-api
  (:require [reagent.core :as r]
            [habits.frontend.auth :as auth]
            [habits.frontend.http :as http]
            [habits.frontend.config :as config]))

(defonce habits-state
         (r/atom {:habits   []
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
  (swap! habits-state assoc
         :error    (.-message err)
         :loading? false))

(defn fetch-habits! []
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str config/api-url "/api/habits")
                #js{:headers (auth-headers)})
      (.then http/check-response!)
      (.then (fn [data]
               (swap! habits-state assoc
                      :habits   (js->clj data :keywordize-keys true)
                      :loading? false)))
      (.catch handle-error!)))

(defn create-habit! [title description color]
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str config/api-url "/api/habits")
                #js{:method  "POST"
                    :headers (auth-headers-json)
                    :body    (js/JSON.stringify (clj->js {:title       title
                                                          :description description
                                                          :color       color}))})
      (.then http/check-response!)
      (.then (fn [new-habit]
               (swap! habits-state
                      (fn [state]
                        (-> state
                            (update :habits conj (js->clj new-habit :keywordize-keys true))
                            (assoc :loading? false))))))
      (.catch handle-error!)))

(defn update-habit! [habit-id updates]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str config/api-url "/api/habits/" habit-id)
                #js{:method  "PATCH"
                    :headers (auth-headers-json)
                    :body    (js/JSON.stringify (clj->js updates))})
      (.then http/check-response!)
      (.then (fn [updated-habit]
               (let [clean-habit (js->clj updated-habit :keywordize-keys true)]
                 (swap! habits-state
                        (fn [state]
                          (-> state
                              (update :habits (fn [habits]
                                                (mapv #(if (= (:id %) habit-id)
                                                         (merge % clean-habit)
                                                         %)
                                                      habits)))
                              (assoc :loading? false)))))))
      (.catch handle-error!)))

(defn delete-habit! [habit-id]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str config/api-url "/api/habits/" habit-id)
                #js{:method  "DELETE"
                    :headers (auth-headers)})
      (.then (fn [response]
               (when-not (.-ok response)
                 (http/throw-error-response! response))))
      (.then (fn [_]
               (swap! habits-state
                      (fn [state]
                        (-> state
                            (update :habits (fn [habits]
                                              (vec (remove #(= (:id %) habit-id) habits))))
                            (assoc :loading? false))))))
      (.catch handle-error!)))