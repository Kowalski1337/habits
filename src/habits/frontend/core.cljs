(ns habits.frontend.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]))

(defonce app-state
         (r/atom {:users []
                  :loading? false
                  :message "Click button to fetch users"}))

(def api-url "http://localhost:3000")

(defn fetch-users! []
  (swap! app-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/users"))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))))
      (.then (fn [data]
               (let [users-array (js->clj data :keywordize-keys true)]
                 (swap! app-state assoc
                        :users users-array
                        :message (str "Found " (count users-array) " users")
                        :loading? false))))
      (.catch (fn [err]
                (swap! app-state assoc
                       :message (str "Error: " (.-message err))
                       :loading? false)))))

(defn user-item [user]
  [:li.border-b.p-2.mt-2
   [:div
    [:span.font-bold "ID: "] (or (:users/id user) "?") [:br]
    [:span.font-bold "Name: "] (or (:users/name user) "N/A") [:br]
    [:span.font-bold "Created: "] (or (:users/created_at user) "N/A")]])

(defn app []
  (let [state @app-state]
    [:div.container.mx-auto.p-8
     [:h1.text-2xl.font-bold "Habits Tracker"]

     [:button.bg-blue-500.text-white.px-4.py-2.rounded.mt-4
      {:on-click fetch-users!
       :disabled (:loading? state)}
      (if (:loading? state) "Loading..." "Fetch Users")]

     [:p.mt-4 (:message state)]

     [:ul.mt-4
      (for [user (:users state)]
        (let [user-id (or (get user "users/id") (rand))]
          ^{:key user-id}
          [user-item user]))]]))

(defn init []
  (rdom/render [app] (js/document.getElementById "app")))

(defn ^:dev/after-load reload []
  (init))