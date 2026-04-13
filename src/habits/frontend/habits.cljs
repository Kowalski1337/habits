(ns habits.frontend.habits
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [habits.frontend.habits-api :as api]
            [habits.frontend.calendar :as calendar]))

(def ^:private initial-form-state
  {:title       ""
   :description ""
   :show-form?  false})

(defn add-habit-form []
  (let [form-state (r/atom initial-form-state)]
    (fn []
      [:div.mb-6
       (if (:show-form? @form-state)
         [:div.rounded-lg.p-4
          {:style {:background-color "#1E2A47"
                   :border           "1px solid #2d3f5e"}}
          [:h3.text-lg.font-semibold.mb-3
           {:style {:color "#cdd6f4"}} "Add New Habit"]

          [:div.mb-3
           [:label.block.text-sm.font-medium.mb-1
            {:style {:color "#a6adc8"}} "Title"]
           [:input.w-full.px-3.py-2.rounded-lg
            {:type        "text"
             :value       (:title @form-state)
             :placeholder "e.g., Morning Meditation"
             :on-change   #(swap! form-state assoc :title (-> % .-target .-value))}]]

          [:div.mb-3
           [:label.block.text-sm.font-medium.mb-1
            {:style {:color "#a6adc8"}} "Description (optional)"]
           [:textarea.w-full.px-3.py-2.rounded-lg
            {:rows        2
             :value       (:description @form-state)
             :placeholder "Describe your habit..."
             :on-change   #(swap! form-state assoc :description (-> % .-target .-value))}]]

          [:div.flex.gap-2
           [:button.px-4.py-2.rounded.font-medium
            {:style    {:background-color "#a6e3a1" :color "#1e1e2e"}
             :on-click (fn []
                         (when-not (str/blank? (:title @form-state))
                           (api/create-habit! (:title @form-state)
                                              (:description @form-state))
                           (reset! form-state initial-form-state)))}
            "Save"]
           [:button.px-4.py-2.rounded
            {:style    {:background-color "#2d3f5e" :color "#cdd6f4"}
             :on-click #(swap! form-state assoc :show-form? false)}
            "Cancel"]]]

         [:button.px-4.py-2.rounded.font-medium
          {:style    {:background-color "#cba6f7" :color "#1e1e2e"}
           :on-click #(swap! form-state assoc :show-form? true)}
          "+ Add New Habit"])])))

(defn habit-item [habit]
  (let [editing?   (r/atom false)
        edit-title (r/atom (:title habit))]
    (fn [habit]
      [:li.p-3.rounded-lg.cursor-pointer
       {:style {:transition "background-color 0.15s"}
        :on-mouse-over #(set! (-> % .-currentTarget .-style .-backgroundColor) "#1E2A47")
        :on-mouse-out  #(set! (-> % .-currentTarget .-style .-backgroundColor) "transparent")}
       (if @editing?
         [:div
          [:input.w-full.px-2.py-1.rounded
           {:type         "text"
            :value        @edit-title
            :auto-focus   true
            :on-change    #(reset! edit-title (-> % .-target .-value))
            :on-key-press (fn [e]
                            (when (= "Enter" (.-key e))
                              (api/update-habit! (:id habit) {:title @edit-title})
                              (reset! editing? false)))}]
          [:div.flex.gap-2.mt-2
           [:button.text-sm.px-3.py-1.rounded
            {:style    {:background-color "#a6e3a1" :color "#1e1e2e"}
             :on-click (fn []
                         (api/update-habit! (:id habit) {:title @edit-title})
                         (reset! editing? false))}
            "Save"]
           [:button.text-sm.px-3.py-1.rounded
            {:style    {:background-color "#2d3f5e" :color "#cdd6f4"}
             :on-click #(reset! editing? false)}
            "Cancel"]]]

         [:div.flex.justify-between.items-center
          [:div.flex-1
           [:span.font-medium {:style {:color "#cdd6f4"}} (:title habit)]
           (when (:description habit)
             [:p.text-sm.mt-1 {:style {:color "#a6adc8"}} (:description habit)])]
          [:div.flex.gap-2
           [:button.text-sm
            {:style    {:color "#89b4fa"}
             :on-click #(do (reset! edit-title (:title habit))
                            (reset! editing? true))}
            "Edit"]
           [:button.text-sm
            {:style    {:color "#f38ba8"}
             :on-click #(api/delete-habit! (:id habit))}
            "Delete"]]])])))

(defn habits-list []
  (let [habits @api/habits-state]
    (cond
      (:loading? habits)
      [:div.text-center.py-8 {:style {:color "#a6adc8"}} "Loading..."]

      (:error habits)
      [:div.p-3.rounded {:style {:background-color "#f38ba8" :color "#1e1e2e"}} (:error habits)]

      (empty? (:habits habits))
      [:div.text-center.py-8 {:style {:color "#a6adc8"}} "No habits yet. Create your first habit!"]

      :else
      [:ul
       (for [habit (:habits habits)]
         ^{:key (:id habit)}
         [habit-item habit])])))

(defn habits-page []
  (r/with-let [_ (api/fetch-habits!)]
              [:div.flex.gap-6.items-start
               [:div.w-180.flex-shrink-0.rounded-xl.p-4
                {:style {:background-color "#112240"
                         :border           "1px solid #2d3f5e"}}
                [:h2.text-xl.font-semibold.mb-3 {:style {:color "#cdd6f4"}} "My Habits"]
                [add-habit-form]
                [habits-list]]
               [:div.flex-1
                {:style {:max-width "400px"}}
                [calendar/calendar]]]))