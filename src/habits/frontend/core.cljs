(ns habits.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [habits.frontend.auth :as auth]
            [habits.frontend.habits :as habits]))

(defonce root (atom nil))

(defn user-panel []
  (let [current-user (:current-user @auth/auth-state)]
    [:div.flex.justify-end
     [:div.text-right
      [:div.font-medium {:style {:color "#cdd6f4"}} (:name current-user)]
      [:button.text-sm.mt-1
       {:style    {:color "#f38ba8"}
        :on-click #(auth/logout!)}
       "Log out"]]]))

(defn app []
  (let [current-user (:current-user @auth/auth-state)]
    [:div.container.mx-auto.p-8
     {:style {:background-color "#0A192F"
              :min-height       "100vh"
              :color            "#E0E0E0"}}
     (if current-user
       [:div
        [:div.flex.justify-between.items-center.mb-8
         [:h1.text-3xl.font-bold {:style {:color "#cba6f7"}} "Habits Tracker"]
         [user-panel]]
        [habits/habits-page]]
       [auth/auth-form])]))

(defn- inject-styles! []
  (let [link (js/document.createElement "link")]
    (set! (.-rel link) "stylesheet")
    (set! (.-href link) "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap")
    (.appendChild js/document.head link))
  (let [style (js/document.createElement "style")]
    (set! (.-textContent style)
          "* { font-family: 'Inter', sans-serif; }
           body { background-color: #0A192F; color: #E0E0E0; }
           .app-container { background-color: #003366; }
           .card { background-color: #1E1E1E; border-color: #45475a; }
           input, textarea, select { background-color: #45475a !important; color: #cdd6f4 !important; border-color: #585b70 !important; }
           button { transition: opacity 0.15s; }")
    (.appendChild js/document.head style)))


(defn init []
  (inject-styles!)
  (let [container (js/document.getElementById "app")]
    (when (nil? @root)
      (reset! root (rdom/create-root container)))
    (.render @root (r/as-element [app]))))

(defn ^:dev/after-load reload []
  (.render @root (r/as-element [app])))