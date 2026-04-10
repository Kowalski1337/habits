(ns habits.frontend.auth
  (:require [reagent.core :as r]))

(defonce auth-state
         (r/atom {:current-user nil
                  :loading? false
                  :error nil}))

(def api-url "http://localhost:3000")

;; Сохранение пользователя
(defn save-user! [user]
  (js/localStorage.setItem "current-user" (js/JSON.stringify (clj->js user)))
  (swap! auth-state assoc :current-user user))

;; Загрузка пользователя
(defn load-user! []
  (let [stored-user (js/localStorage.getItem "current-user")]
    (if stored-user
      (try
        (let [user (js->clj (js/JSON.parse stored-user) :keywordize-keys true)]
          (swap! auth-state assoc :current-user user)
          user)
        (catch js/Error _
          (js/localStorage.removeItem "current-user")
          nil))
      nil)))

;; Получение текущего пользователя
(defn get-current-user []
  (:current-user @auth-state))

;; Очистка сессии
(defn logout! []
  (js/localStorage.removeItem "current-user")
  (swap! auth-state assoc :current-user nil :error nil))

;; Логин или регистрация (единая функция)
(defn login! [name password]
  (swap! auth-state assoc :loading? true :error nil)
  (-> (js/fetch (str api-url "/api/auth/login")
                #js{:method "POST"
                    :headers #js{"Content-Type" "application/json"}
                    :body (js/JSON.stringify (clj->js {:name name :password password}))})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (-> (.json response)
                     (.then (fn [error-data]
                              (throw (js/Error. (.-message error-data)))))))))
      (.then (fn [data]
               (let [result (js->clj data :keywordize-keys true)
                     user (:user result)]
                 (save-user! user)
                 (swap! auth-state assoc :loading? false))))
      (.catch (fn [err]
                (swap! auth-state assoc
                       :error (.-message err)
                       :loading? false)))))

;; Компонент формы авторизации
(defn auth-form []
  (let [local-state (r/atom {:name "" :password ""})]
    (fn []
      (let [loading? (:loading? @auth-state)
            error (:error @auth-state)]
        [:div.max-w-md.mx-auto.mt-20.p-6.bg-white.rounded-lg.shadow-md
         [:h2.text-2xl.font-bold.text-center.mb-6 "Login"]

         [:div.mb-4
          [:label.block.text-gray-700.mb-2 "Name"]
          [:input.w-full.px-3.py-2.border.rounded-lg
           {:type "text"
            :value (:name @local-state)
            :on-change #(swap! local-state assoc :name (-> % .-target .-value))
            :on-key-press (fn [e]
                            (when (= "Enter" (.-key e))
                              (login! (:name @local-state) (:password @local-state))))}]]

         [:div.mb-4
          [:label.block.text-gray-700.mb-2 "Password"]
          [:input.w-full.px-3.py-2.border.rounded-lg
           {:type "password"
            :value (:password @local-state)
            :on-change #(swap! local-state assoc :password (-> % .-target .-value))
            :on-key-press (fn [e]
                            (when (= "Enter" (.-key e))
                              (login! (:name @local-state) (:password @local-state))))}]]

         (when error
           [:div.mb-4.p-3.bg-red-100.border.border-red-400.text-red-700.rounded
            error])

         [:button.w-full.bg-blue-500.text-white.py-2.rounded-lg.hover:bg-blue-600
          {:on-click #(login! (:name @local-state) (:password @local-state))
           :disabled loading?}
          (if loading? "Loading..." "Login")]]))))

;; Инициализация
(load-user!)