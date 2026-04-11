(ns habits.frontend.calendar
  (:require [habits.frontend.habits-api :as habits-api]
            [habits.frontend.logs-api :as logs-api]
            [reagent.core :as r]))

(def emotion-colors
  [{:value "#10B981" :label "Happiness"}
   {:value "#3B82F6" :label "Calmness"}
   {:value "#F59E0B" :label "Energy"}
   {:value "#EF4444" :label "Anger"}
   {:value "#8B5CF6" :label "Sadness"}
   {:value "#000000" :label "Not completed"}])

(defn- today [] (js/Date.))

(defn- date->str [year month day]
  (str year "-"
       (-> (inc month) str (.padStart 2 "0"))
       "-"
       (-> day str (.padStart 2 "0"))))

(defn- days-in-month [year month]
  (.getDate (js/Date. year (inc month) 0)))

(defn- first-day-of-month [year month]
  (let [day (.getDay (js/Date. year month 1))]
    (if (= day 0) 6 (dec day))))

(def ^:private month-names
  ["January" "February" "Mart" "April" "May" "June"
   "July" "August" "September" "October" "November" "December"])

(def ^:private day-names ["Mo" "Tu" "We" "Th" "Fr" "Sa" "Su"])


(defn- basic-modal [z-index content]
  (fn []
    [:div.fixed.inset-0.pointer-events-none
     {:style {:z-index z-index}}
     [:div.absolute.bg-white.rounded-xl.shadow-xl.pointer-events-auto
      {:style {:left      "50%"
               :top       "50%"
               :transform "translate(-50%, -50%)"
               :min-width "400px"}}
      content]]))

(defn- habit-log-modal [habit date-str log on-close]
  (let [local-state (r/atom {:completed     (boolean (:completed log))
                             :emotion-color (or (:emotion-color log) "")})]
    (fn []
      (let [{:keys [completed emotion-color]} @local-state]
        [basic-modal 200
         [:div.p-6 {:style {:min-width "360px"}}
          [:div.flex.justify-between.items-center.mb-4.cursor-grab.select-none
           [:h3.text-lg.font-semibold (:title habit)]
           [:button.text-gray-400.hover:text-gray-600.text-xl
            {:on-click on-close} "✕"]]

          (when (:description habit)
            [:p.text-sm.text-gray-500.mb-4 (:description habit)])

          [:div.flex.items-center.gap-3.mb-4
           [:label.flex.items-center.gap-2.cursor-pointer
            [:input.w-4.h-4
             {:type      "checkbox"
              :checked   completed
              :on-change (fn [e]
                           (let [checked? (-> e .-target .-checked)]
                             (swap! local-state assoc
                                    :completed     checked?
                                    :emotion-color (if checked? emotion-color ""))))}]
            [:span.text-sm "Выполнено"]]]

          [:div.mb-6
           [:label.block.text-sm.font-medium.mb-2 "Эмоция"]
           [:div.grid.grid-cols-3.gap-2
            (for [{:keys [value label]} emotion-colors]
              ^{:key value}
              [:button.text-xs.py-2.px-1.rounded-lg.border-2.transition-all
               {:class    (when (= value emotion-color) "ring-2 ring-offset-1 ring-gray-800")
                :style    {:background-color value
                           :color            (if (= value "#000000") "white" "black")
                           :border-color     (if (= value emotion-color) "#1f2937" "transparent")
                           :opacity          (if (not completed) "0.4" "1")}
                :disabled (not completed)
                :on-click #(swap! local-state assoc :emotion-color value)}
               label])]]

          [:div.flex.justify-end.gap-2
           [:button.px-4.py-2.text-sm.bg-gray-200.rounded.hover:bg-gray-300
            {:on-click on-close} "Отмена"]
           [:button.px-4.py-2.text-sm.bg-blue-500.text-white.rounded.hover:bg-blue-600
            {:on-click (fn []
                         (logs-api/upsert-log!
                           (:id habit) date-str completed emotion-color)
                         (on-close))}
            "Сохранить"]]]]))))

(defn- day-modal [date-str on-close]
  (let [selected-habit (r/atom nil)]
    (fn []
      (let [habits  (:habits @habits-api/habits-state)
            logs    (get-in @logs-api/logs-state [:logs date-str])
            log-map (into {} (map (fn [l] [(:habit-id l) l]) (or logs [])))]
        [:<>
         [basic-modal 100
          [:div.p-6 {:style {:min-width "400px"}}
           [:div.flex.justify-between.items-center.mb-4.cursor-grab.select-none
            [:h3.text-lg.font-semibold date-str]
            [:button.text-gray-400.hover:text-gray-600.text-xl
             {:on-click on-close
              :style    {:cursor "default"}} "✕"]]

           [:div {:style {:pointer-events (if @selected-habit "none" "auto")
                          :opacity        (if @selected-habit "0.5" "1")
                          :transition     "opacity 0.15s"}}
            (if (empty? habits)
              [:p.text-gray-400.text-sm.text-center "Нет привычек. Создайте первую!"]
              [:ul.space-y-2
               (for [habit habits]
                 (let [log        (get log-map (:id habit))
                       completed? (boolean (:completed log))
                       color      (when completed? (:emotion-color log))]
                   ^{:key (:id habit)}
                   [:li.rounded-lg.p-3.cursor-pointer.transition-all.border
                    {:style    {:background-color (or color "transparent")}
                     :class    (if completed?
                                 "border-transparent"
                                 "border-gray-200 hover:border-gray-300 hover:bg-gray-50")
                     :on-click #(reset! selected-habit habit)}
                    [:div.text-center
                     [:div.font-medium.text-sm (:title habit)]
                     (when (:description habit)
                       [:div.text-xs.text-gray-500.mt-0.5 (:description habit)])]]))])]]]

         (when @selected-habit
           [habit-log-modal
            @selected-habit
            date-str
            (get log-map (:id @selected-habit))
            #(reset! selected-habit nil)])]))))

(defn- day-dots [date-str]
  (let [logs (get-in @logs-api/logs-state [:logs date-str])
        habits (:habits @habits-api/habits-state)
        log-map (into {} (map (fn [l] [(:habit-id l) l]) (or logs [])))]
    [:div.flex.flex-wrap.gap-0.5.justify-center.mt-1
     (for [habit habits]
       (let [log (get log-map (:id habit))
             completed? (and log (:completed log))]
         ^{:key (:id habit)}
         [:div.rounded-full
          {:style {:width            "6px"
                   :height           "6px"
                   :background-color (if completed?
                                       (or (:emotion-color log) "#6B7280")
                                       "#000000")
                   :opacity          (if completed? "1" "0.2")}}]))]))

(defn- day-cell [year month day selected-date]
  (let [t (today)
        date-str (date->str year month day)
        today-str (date->str (.getFullYear t) (.getMonth t) (.getDate t))
        is-today? (= date-str today-str)]
    [:div.flex.flex-col.items-center.cursor-pointer.rounded-lg.p-1
     {:class    (str (when is-today? "bg-blue-50 ") "hover:bg-gray-100 transition-colors")
      :on-click #(reset! selected-date date-str)}
     [:span.text-sm {:class (when is-today? "text-blue-600 font-bold")} day]
     [day-dots date-str]]))

(defn- month-picker [cur-year cur-month on-change]
  (let [open? (r/atom false)
        t (today)
        base-year (.getFullYear t)]
    (fn []
      [:div.relative
       [:button.text-sm.border.rounded.px-2.py-1.hover:bg-gray-50
        {:on-click #(swap! open? not)}
        (str (get month-names cur-month) " " cur-year " ▾")]
       (when @open?
         [:<>
          [:div.fixed.inset-0.z-10
           {:on-click #(reset! open? false)}]
          [:div.absolute.top-8.left-0.bg-white.border.rounded-lg.shadow-lg.p-3.z-20
           {:style {:width "220px" :max-height "300px" :overflow-y "auto"}}
           (for [year (range (- base-year 5) (+ base-year 6))]
             ^{:key year}
             [:div.mb-2
              [:div.text-xs.font-semibold.text-gray-500.mb-1 year]
              [:div.grid.grid-cols-4.gap-1
               (for [m (range 12)]
                 ^{:key m}
                 [:button.text-xs.px-1.py-1.rounded.hover:bg-blue-100
                  {:class    (when (and (= year cur-year) (= m cur-month))
                               "bg-blue-500 text-white")
                   :on-click (fn []
                               (on-change year m)
                               (reset! open? false))}
                  (subs (get month-names m) 0 3)])]])]])])))

(defn calendar []
  (let [t (today)
        cur-year (r/atom (.getFullYear t))
        cur-month (r/atom (.getMonth t))
        selected-date (r/atom nil)]
    (fn []
      (let [year @cur-year
            month @cur-month
            days (days-in-month year month)
            first-day (first-day-of-month year month)]

        (logs-api/fetch-logs! year (inc month))

        [:div.bg-white.rounded-xl.border.p-4
         [:div.flex.items-center.justify-between.mb-4
          [:button.text-gray-500.hover:text-gray-800.px-2.py-1.rounded.hover:bg-gray-100
           {:on-click (fn []
                        (if (zero? month)
                          (do (reset! cur-year (dec year))
                              (reset! cur-month 11))
                          (swap! cur-month dec)))}
           "◄"]
          [:div.flex.items-center.gap-2
           [month-picker year month
            (fn [y m]
              (reset! cur-year y)
              (reset! cur-month m))]
           [:button.text-sm.text-blue-500.hover:text-blue-700.px-2.py-1.rounded.hover:bg-blue-50
            {:on-click (fn []
                         (let [t (today)]
                           (reset! cur-year (.getFullYear t))
                           (reset! cur-month (.getMonth t))))}
            "Сегодня"]]
          [:button.text-gray-500.hover:text-gray-800.px-2.py-1.rounded.hover:bg-gray-100
           {:on-click (fn []
                        (if (= month 11)
                          (do (reset! cur-year (inc year))
                              (reset! cur-month 0))
                          (swap! cur-month inc)))}
           "►"]]

         [:div.grid.grid-cols-7.mb-1
          (for [d day-names]
            ^{:key d}
            [:div.text-center.text-xs.text-gray-400.font-medium.py-1 d])]

         [:div.grid.grid-cols-7.gap-1
          (for [i (range first-day)]
            ^{:key (str "empty-" i)}
            [:div.p-1])
          (for [day (range 1 (inc days))]
            ^{:key day}
            [day-cell year month day selected-date])]

         (when @selected-date
           [day-modal @selected-date #(reset! selected-date nil)])]))))