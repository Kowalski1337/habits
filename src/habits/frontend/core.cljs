(ns habits.frontend.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [habits.frontend.auth :as auth]
            [habits.frontend.habits :as habits]))

(defonce app-state
         (r/atom {:current-user nil
                  :habits-page-loaded? false}))

(defn sync-current-user []
  (let [current-user (auth/get-current-user)]
    (when (not= current-user (:current-user @app-state))
      (swap! app-state assoc :current-user current-user))))

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

  (let [current-user (:current-user @app-state)]

    [:div.container.mx-auto.p-8
     (if current-user
       [:div
        [user-panel]
        [:h1.text-3xl.font-bold.text-center.mb-8 "Habits Tracker"]
        [habits/habits-page (:id current-user)]]

       [auth/auth-form])]))

(defn init []
  (auth/load-user!)
  (sync-current-user)
  (rdom/render [app] (js/document.getElementById "app")))

(defn ^:dev/after-load reload []
  (init))