(ns habits.frontend.core
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]))

(defonce state (r/atom {:message "Loading..."}))

(defn fetch-test []
      (-> (js/fetch "http://localhost:3000/api/test")
          (.then #(.json %))
          (.then (fn [data]
                     (swap! state assoc :message (.-message data))))
          (.catch (fn [err]
                      (swap! state assoc :message (str "Error: " err))))))

(defn app []
  (let [state @state]  ;; ← читаем атом один раз в начале
    [:div.container.mx-auto.p-8
     [:h1.text-3xl.font-bold.text-blue-600 "Habit Tracker"]
     [:p.mt-4 "Message from server: " (:message state)]
     [:button.bg-green-500.text-white.px-4.py-2.rounded.mt-4
      {:on-click fetch-test}
      "Fetch from API"]]))

(defn init []
      (rdom/render [app] (js/document.getElementById "app")))

(defn ^:dev/after-load reload []
      (init))