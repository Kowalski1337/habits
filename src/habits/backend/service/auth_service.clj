(ns habits.backend.service.auth-service
  (:require [buddy.hashers :as hashers]
            [habits.backend.dao.users-dao :as users-dao]
            [habits.backend.entity.user-entity :as entity]
            [habits.backend.response :as resp]))

(defn login-or-register!
  [req]
  (let [body (:body req)
        name (:name body)
        password (:password body)]

    (cond
      (or (nil? name) (clojure.string/blank? name))
      (resp/bad-request "Name is required" {:field "name"})

      (or (nil? password) (clojure.string/blank? password))
      (resp/bad-request "Password is required" {:field "password"})

      (< (count password) 3)
      (resp/bad-request "Password must be at least 3 characters" {:field "password"})

      :else
      (let [find-result (users-dao/find-user-by-name name)]
        (if (:success find-result)
          (let [user (:data find-result)
                password-hash (:users/password_hash user)]
            (if (hashers/check password password-hash)
              (resp/success {:user (entity/build user)
                             :message "Login successful"})
              (resp/unauthorized "Invalid password or name already taken")))

          (let [password-hash (hashers/derive password)
                create-result (users-dao/create-user! name password-hash)]
            (if (:success create-result)
              (resp/success {:user (-> create-result :data entity/build)
                             :message "User created and logged in"})
              (resp/server-error "Failed to create user"))))))))