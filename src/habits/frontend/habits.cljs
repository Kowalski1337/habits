(ns habits.frontend.habits
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [habits.frontend.habits-api :as api]))

(def ^:private initial-form-state
  {:title       ""
   :description ""
   :color       "#3B82F6"
   :show-form?  false})

(defn add-habit-form []
  (let [form-state (r/atom initial-form-state)]
    (fn []
      [:div.mb-6
       (if (:show-form? @form-state)
         [:div.border.rounded-lg.p-4.bg-gray-50
          [:h3.text-lg.font-semibold.mb-3 "Add New Habit"]

          [:div.mb-3
           [:label.block.text-sm.font-medium.mb-1 "Title"]
           [:input.w-full.px-3.py-2.border.rounded-lg
            {:type        "text"
             :value       (:title @form-state)
             :placeholder "e.g., Morning Meditation"
             :on-change   #(swap! form-state assoc :title (-> % .-target .-value))}]]

          [:div.mb-3
           [:label.block.text-sm.font-medium.mb-1 "Description (optional)"]
           [:textarea.w-full.px-3.py-2.border.rounded-lg
            {:rows        2
             :value       (:description @form-state)
             :placeholder "Describe your habit..."
             :on-change   #(swap! form-state assoc :description (-> % .-target .-value))}]]

          [:div.flex.gap-2
           [:button.bg-green-500.text-white.px-4.py-2.rounded.hover:bg-green-600
            {:on-click (fn []
                         (when-not (str/blank? (:title @form-state))
                           (api/create-habit! (:title @form-state)
                                              (:description @form-state))
                           (reset! form-state initial-form-state)))}
            "Save"]
           [:button.bg-gray-300.text-gray-700.px-4.py-2.rounded.hover:bg-gray-400
            {:on-click #(swap! form-state assoc :show-form? false)}
            "Cancel"]]]

         [:button.bg-blue-500.text-white.px-4.py-2.rounded.hover:bg-blue-600
          {:on-click #(swap! form-state assoc :show-form? true)}
          "+ Add New Habit"])])))

(defn habit-item [habit]
  (let [editing?   (r/atom false)
        edit-title (r/atom (:title habit))]
    (fn [habit]
      [:li.border-b.p-3.hover:bg-gray-50
       (if @editing?
         [:div
          [:input.w-full.px-2.py-1.border.rounded
           {:type         "text"
            :value        @edit-title
            :auto-focus   true
            :on-change    #(reset! edit-title (-> % .-target .-value))
            :on-key-press (fn [e]
                            (when (= "Enter" (.-key e))
                              (api/update-habit! (:id habit) {:title @edit-title})
                              (reset! editing? false)))}]
          [:div.flex.gap-2.mt-2
           [:button.text-sm.bg-green-500.text-white.px-3.py-1.rounded
            {:on-click (fn []
                         (api/update-habit! (:id habit) {:title @edit-title})
                         (reset! editing? false))}
            "Save"]
           [:button.text-sm.bg-gray-300.text-gray-700.px-3.py-1.rounded
            {:on-click #(reset! editing? false)}
            "Cancel"]]]

         [:div.flex.justify-between.items-center
          [:div.flex-1
           [:div.flex.items-center.gap-2
            [:div.w-3.h-3.rounded-full
             {:style {:background-color (:color habit)}}]
            [:span.font-medium (:title habit)]]
           (when (:description habit)
             [:p.text-sm.text-gray-500.mt-1 (:description habit)])]
          [:div.flex.gap-2
           [:button.text-blue-500.hover:text-blue-700.text-sm
            {:on-click #(do (reset! edit-title (:title habit))
                            (reset! editing? true))}
            "Edit"]
           [:button.text-red-500.hover:text-red-700.text-sm
            {:on-click #(api/delete-habit! (:id habit))}
            "Delete"]]])])))

(defn habits-list []
  (let [habits @api/habits-state]
    (cond
      (:loading? habits)
      [:div.text-center.py-8 "Loading..."]

      (:error habits)
      [:div.bg-red-100.text-red-700.p-3.rounded (:error habits)]

      (empty? (:habits habits))
      [:div.text-center.text-gray-500.py-8 "No habits yet. Create your first habit!"]

      :else
      [:ul.divide-y.divide-gray-200
       (for [habit (:habits habits)]
         ^{:key (:id habit)}
         [habit-item habit])])))

(defn habits-page []
  (r/with-let [_ (api/fetch-habits!)]
              [:div
               [add-habit-form]
               [:div.mt-6
                [:h2.text-xl.font-semibold.mb-3 "My Habits"]
                [habits-list]]]))