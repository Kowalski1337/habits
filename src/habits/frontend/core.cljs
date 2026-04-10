(ns habits.frontend.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [habits.frontend.auth :as auth]))

(defonce app-state
         (r/atom {:users []
                  :loading? false
                  :message "Click button to fetch users"
                  :current-user nil}))

(def api-url "http://localhost:3000")

(defn sync-current-user []
  (let [current-user (auth/get-current-user)]
    (when (not= current-user (:current-user @app-state))
      (swap! app-state assoc :current-user current-user))))

(defn fetch-users! []
  (swap! app-state assoc :loading? true)
  (-> (js/fetch (str api-url "/api/users")
                #js{:headers #js{"user-id" (-> @app-state :current-user :id)}})
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
    [:span.font-bold "ID: "] (or (:id user) "?") [:br]
    [:span.font-bold "Name: "] (or (:name user) "N/A")]])

(defn user-panel []
  (let [current-user (:current-user @app-state)]
    [:div.flex.justify-end.mb-6.p-3.border-b.border-gray-200
     [:div.text-right
      [:div.text-gray-700.font-medium (:name current-user)]
      [:button.text-sm.text-red-500.hover:text-red-700.transition-colors.mt-1
       {:on-click #(do (auth/logout!) (sync-current-user))}
       "Log out"]]]))

(defn app []
  (sync-current-user)

  (let [state @app-state
        current-user (:current-user state)]

    [:div.container.mx-auto.p-8
     (if current-user
       [:div
        [user-panel]

        [:h1.text-2xl.font-bold "Habits Tracker"]

        [:button.bg-blue-500.text-white.px-4.py-2.rounded.mt-4
         {:on-click fetch-users!
          :disabled (:loading? state)}
         (if (:loading? state) "Loading..." "Fetch Users")]

        [:p.mt-4 (:message state)]

        [:ul.mt-4
         (for [user (:users state)]
           (let [user-id (or (:users/id user) (rand))]
             ^{:key user-id}
             [user-item user]))]]

       [auth/auth-form])]))

(defn init []
  (auth/load-user!)
  (sync-current-user)
  (rdom/render [app] (js/document.getElementById "app")))

(defn ^:dev/after-load reload []
  (init))