(ns habits.backend.api-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [habits.backend.core :refer [app-routes wrap-base]]
            [habits.backend.dao.habits-dao :as habits-dao]
            [habits.backend.dao.users-dao :as users-dao]
            [ring.mock.request :as mock]))

(def ^:private handler (wrap-base app-routes))

(defn- get-req [url & {:keys [user-id]}]
  (cond-> (mock/request :get url)
          user-id (mock/header "user-id" (str user-id))))

(defn- post-req [url body & {:keys [user-id]}]
  (cond-> (mock/request :post url)
          true (mock/content-type "application/json")
          true (mock/body (json/generate-string body))
          user-id (mock/header "user-id" (str user-id))))

(defn- patch-req [url body & {:keys [user-id]}]
  (cond-> (mock/request :patch url)
          true (mock/content-type "application/json")
          true (mock/body (json/generate-string body))
          user-id (mock/header "user-id" (str user-id))))

(defn- delete-req [url & {:keys [user-id]}]
  (cond-> (mock/request :delete url)
          user-id (mock/header "user-id" (str user-id))))

(defn- parse-body [resp]
  (when-let [body (:body resp)]
    (cond
      (string? body) (json/parse-string body true)
      (map? body) body
      :else (json/parse-string (slurp body) true))))

(defn- status [resp] (:status resp))

(def ^:private mock-user-name "Elochka")

(def ^:private mock-habit
  {:id 1 :title "Run" :description "Morning run"})

(def ^:private mock-user
  {:id 1 :name mock-user-name})

(deftest get-habits-test
  (testing "Get valid"
    (with-redefs [habits-dao/get-user-habits
                  (fn [_] {:success true :data [mock-habit]})]
      (let [resp (handler (get-req "/api/habits" :user-id 1))]
        (is (= 200 (status resp)))
        (is (= [mock-habit] (parse-body resp))))))

  (testing "No habits"
    (with-redefs [habits-dao/get-user-habits
                  (fn [_] {:success true :data []})]
      (let [resp (handler (get-req "/api/habits" :user-id 1))]
        (is (= 200 (status resp)))
        (is (= [] (parse-body resp))))))

  (testing "User id is missing"
    (let [resp (handler (get-req "/api/habits"))]
      (is (= 400 (status resp)))))

  (testing "User id wrong format"
    (let [resp (handler (get-req "/api/habits" :user-id "abc"))]
      (is (= 400 (status resp)))))

  (testing "Bypass database failure"
    (with-redefs [habits-dao/get-user-habits
                  (fn [_] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (handler (get-req "/api/habits" :user-id 1))]
        (is (= 500 (status resp)))))))

(deftest create-habit-test
  (testing "Create valid"
    (with-redefs [habits-dao/create-habit!
                  (fn [& _] {:success true :data mock-habit})]
      (let [resp (handler (post-req "/api/habits"
                                    {:title "Run"}
                                    :user-id 1))]
        (is (= 201 (status resp)))
        (is (= (:title mock-habit) (:title (parse-body resp)))))))

  (testing "Missing title"
    (let [resp (handler (post-req "/api/habits" {} :user-id 1))]
      (is (= 400 (status resp)))))

  (testing "Empty title"
    (let [resp (handler (post-req "/api/habits" {:title ""} :user-id 1))]
      (is (= 400 (status resp)))))

  (testing "Still empty title"
    (let [resp (handler (post-req "/api/habits" {:title "   "} :user-id 1))]
      (is (= 400 (status resp)))))

  (testing "User id is missing"
    (let [resp (handler (post-req "/api/habits" {:title "Run"}))]
      (is (= 400 (status resp)))))

  (testing "Bypass databse failure"
    (with-redefs [habits-dao/create-habit!
                  (fn [& _] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (handler (post-req "/api/habits" {:title "Run"} :user-id 1))]
        (is (= 500 (status resp)))))))

(deftest update-habit-test
  (testing "Update valid"
    (with-redefs [habits-dao/update-habit!
                  (fn [& _] {:success true :data mock-habit})]
      (let [resp (handler (patch-req "/api/habits/1" {:title "New title"} :user-id 1))]
        (is (= 200 (status resp))))))

  (testing "Empty update"
    (let [resp (handler (patch-req "/api/habits/1" {} :user-id 1))]
      (is (= 400 (status resp)))))

  ;; TODO figure out if possible to rule out on route level
  (testing "Habit  id  wrong  format"
    (let [resp (handler (patch-req "/api/habits/abc" {:title "X"} :user-id 1))]
      (is (= 400 (status resp)))))

  (testing "User id is missing"
    (let [resp (handler (patch-req "/api/habits/1" {:title "X"}))]
      (is (= 400 (status resp)))))

  (testing "Habit not found"
    (with-redefs [habits-dao/update-habit!
                  (fn [& _] {:success false :error-code :not-found
                             :message "Not found" :details {:id 1}})]
      (let [resp (handler (patch-req "/api/habits/1" {:title "X"} :user-id 1))]
        (is (= 404 (status resp))))))

  (testing "Bypass database failure"
    (with-redefs [habits-dao/update-habit!
                  (fn [& _] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (handler (patch-req "/api/habits/1" {:title "X"} :user-id 1))]
        (is (= 500 (status resp)))))))

(deftest delete-habit-test
  (testing "Delete valid"
    (with-redefs [habits-dao/delete-habit!
                  (fn [_] {:success true :data {:id 1 :deleted true}})]
      (let [resp (handler (delete-req "/api/habits/1" :user-id 1))]
        (is (= 204 (status resp))))))

  (testing "Habit id wrong format"
    (let [resp (handler (delete-req "/api/habits/abc" :user-id 1))]
      (is (= 400 (status resp)))))

  (testing "User id is missing"
    (let [resp (handler (delete-req "/api/habits/1"))]
      (is (= 400 (status resp)))))

  (testing "Habit not found"
    (with-redefs [habits-dao/delete-habit!
                  (fn [_] {:success false :error-code :not-found
                           :message "Not found" :details {:id 1}})]
      (let [resp (handler (delete-req "/api/habits/1" :user-id 1))]
        (is (= 404 (status resp))))))

  (testing "Bypass database error"
    (with-redefs [habits-dao/delete-habit!
                  (fn [_] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (handler (delete-req "/api/habits/1" :user-id 1))]
        (is (= 500 (status resp)))))))

(deftest auth-login-test
  (testing "Login"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success true :data {:id 1 :name mock-user-name :password-hash "hash"}})
                  buddy.hashers/check
                  (fn [_ _] true)]
      (let [resp (handler (post-req "/api/auth/login" {:name mock-user-name :password "pass"}))]
        (is (= 200 (status resp)))
        (is (= "Login successful" (:message (parse-body resp)))))))

  (testing "Registration"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success false :error-code :not-found :message "Not found"})
                  users-dao/create-user!
                  (fn [& _] {:success true :data mock-user})
                  buddy.hashers/derive
                  (fn [_] "hashed")]
      (let [resp (handler (post-req "/api/auth/login" {:name "VovseNeElochka" :password "pass"}))]
        (is (= 200 (status resp)))
        (is (= "User created and logged in" (:message (parse-body resp)))))))

  (testing "Wrong password"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success true :data {:id 1 :name mock-user-name :password-hash "hash"}})
                  buddy.hashers/check
                  (fn [_ _] false)]
      (let [resp (handler (post-req "/api/auth/login" {:name mock-user-name :password "wrong"}))]
        (is (= 401 (status resp))))))

  (testing "Name is missing"
    (let [resp (handler (post-req "/api/auth/login" {:password "pass"}))]
      (is (= 400 (status resp)))))

  (testing "Password is missing"
    (let [resp (handler (post-req "/api/auth/login" {:name mock-user-name}))]
      (is (= 400 (status resp)))))

  (testing "Too easy password"
    (let [resp (handler (post-req "/api/auth/login" {:name mock-user-name :password "ab"}))]
      (is (= 400 (status resp))))))

(deftest not-found-test
  (testing "Unknown rout"
    (let [resp (handler (get-req "/api/nonexistent"))]
      (is (= 404 (status resp))))))