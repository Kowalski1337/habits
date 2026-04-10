(ns habits.frontend.core
  (:require [reagent.dom :as rdom]
            [habits.frontend.auth :as auth]
            [habits.frontend.habits :as habits]))

(defn user-panel []
  (let [current-user (:current-user @auth/auth-state)]
    [:div.flex.justify-end.mb-6.p-3.border-b.border-gray-200
     [:div.text-right
      [:div.text-gray-700.font-medium (:name current-user)]
      [:button.text-sm.text-red-500.hover:text-red-700.transition-colors.mt-1
       {:on-click #(auth/logout!)}
       "Log out"]]]))

(defn app []
  (let [current-user (:current-user @auth/auth-state)]
    [:div.container.mx-auto.p-8
     (if current-user
       [:div
        [user-panel]
        [:h1.text-3xl.font-bold.text-center.mb-8 "Habits Tracker"]
        [habits/habits-page]]
       [auth/auth-form])]))

(defn init []
  (rdom/render [app] (js/document.getElementById "app")))

(defn ^:dev/after-load reload []
  (init))