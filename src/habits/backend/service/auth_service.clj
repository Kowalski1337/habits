(ns habits.backend.service.auth-service
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [habits.backend.dao.users-dao :as users-dao]
            [habits.backend.response :as resp]))

(defn- blank-field? [v]
  (or (nil? v) (str/blank? v)))

(defn- login! [user password]
  (if (hashers/check password (:password-hash user))
    (resp/success {:user user :message "Login successful"})
    (resp/unauthorized "Invalid password or name already taken")))

(defn- register! [name password]
  (let [result (users-dao/create-user! name (hashers/derive password))]
    (if (:success result)
      (resp/success {:user (:data result) :message "User created and logged in"})
      (resp/server-error "Failed to create user"))))

(defn login-or-register!
  [req]
  (let [{:keys [name password]} (:body req)]

    (cond
      (blank-field? name)
      (resp/bad-request "Name is required" {:field "name"})

      (blank-field? password)
      (resp/bad-request "Password is required" {:field "password"})

      (< (count password) 3)
      (resp/bad-request "Password must be at least 3 characters" {:field "password"})

      :else
      (let [find-result (users-dao/find-user-by-name name)]
        (if (:success find-result)
          (-> find-result :data (login! password))
          (register! name password))))))