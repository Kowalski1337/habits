(ns habits.frontend.habits-api
  (:require [reagent.core :as r]
            [habits.frontend.http :as http]))

(def api-url "http://localhost:3000")

(defonce habits-state
         (r/atom {:habits   []
                  :loading? false
                  :error    nil}))

(defn- handle-error! [err]
  (swap! habits-state assoc
         :error    (.-message err)
         :loading? false))


(defn fetch-habits! [user-id]
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str api-url "/api/habits")
                #js{:headers #js{"user-id" (str user-id)}})
      (.then http/check-response!)
      (.then (fn [data]
               (swap! habits-state assoc
                      :habits   (js->clj data :keywordize-keys true)
                      :loading? false)))
      (.catch handle-error!)))

(defn create-habit! [user-id title description color]
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str api-url "/api/habits")
                #js{:method  "POST"
                    :headers #js{"Content-Type" "application/json"
                                 "user-id"      (str user-id)}
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

(defn update-habit! [user-id habit-id updates]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/habits/" habit-id)
                #js{:method  "PATCH"
                    :headers #js{"Content-Type" "application/json"
                                 "user-id"      (str user-id)}
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

(defn delete-habit! [user-id habit-id]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/habits/" habit-id)
                #js{:method  "DELETE"
                    :headers #js{"user-id" (str user-id)}})
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