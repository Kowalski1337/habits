(ns habits.frontend.habits-api
  (:require [reagent.core :as r]))

(def api-url "http://localhost:3000")

(defonce habits-state
         (r/atom {:habits   []
                  :loading? false
                  :error    nil}))

(defn fetch-habits! [user-id]
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str api-url "/api/habits")
                #js{:headers #js{"user-id" (str user-id)}})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (let [habits-array (js->clj data :keywordize-keys true)]
                 (swap! habits-state assoc
                        :habits habits-array
                        :loading? false))))
      (.catch (fn [err]
                (swap! habits-state assoc
                       :error (.-message err)
                       :loading? false)))))

(defn create-habit! [user-id title description color]
  (swap! habits-state assoc :loading? true :error nil)
  (-> (js/fetch (str api-url "/api/habits")
                #js{:method  "POST"
                    :headers #js{"Content-Type" "application/json"
                                 "user-id"      (str user-id)}
                    :body    (js/JSON.stringify (clj->js {:title       title
                                                          :description description
                                                          :color       color}))})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (-> (.json response)
                     (.then (fn [error-data]
                              (throw (js/Error. (.-message error-data)))))))))
      (.then (fn [new-habit]
               (let [clean-habit (js->clj new-habit :keywordize-keys true)]
                 (swap! habits-state
                        (fn [state]
                          (-> state
                              (update :habits conj clean-habit)
                              (assoc :loading? false)))))))
      (.catch (fn [err]
                (swap! habits-state assoc
                       :error (.-message err)
                       :loading? false)))))

(defn update-habit! [user-id habit-id updates]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/habits/" habit-id)
                #js{:method  "PATCH"
                    :headers #js{"Content-Type" "application/json"
                                 "user-id"      (str user-id)}
                    :body    (js/JSON.stringify (clj->js updates))})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (-> (.json response)
                     (.then (fn [error-data]
                              (throw (js/Error. (.-message error-data)))))))))
      (.then (fn [updated-habit]
               (let [clean-habit (js->clj updated-habit :keywordize-keys true)]
                 (swap! habits-state update :habits
                        (fn [habits]
                          (mapv (fn [h]
                                  (if (= (:id h) habit-id)
                                    (merge h clean-habit)
                                    h))
                                habits)))
                 (swap! habits-state assoc :loading? false))))
      (.catch (fn [err]
                (swap! habits-state assoc
                       :error (.-message err)
                       :loading? false)))))

(defn delete-habit! [user-id habit-id]
  (swap! habits-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/habits/" habit-id)
                #js{:method  "DELETE"
                    :headers #js{"user-id" (str user-id)}})
      (.then (fn [response]
               (if (.-ok response)
                 nil
                 (-> (.json response)
                     (.then (fn [error-data]
                              (throw (js/Error. (.-message error-data)))))))))
      (.then (fn [_]
               (swap! habits-state update :habits
                      (fn [habits]
                        (filterv #(not= (:id %) habit-id) habits)))
               (swap! habits-state assoc :loading? false)))
      (.catch (fn [err]
                (swap! habits-state assoc
                       :error (.-message err)
                       :loading? false)))))