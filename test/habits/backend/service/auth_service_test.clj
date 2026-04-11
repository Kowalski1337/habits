(ns habits.backend.service.auth-service-test
  (:require [clojure.test :refer [deftest is testing]]
            [habits.backend.service.auth-service :as sut]
            [habits.backend.dao.users-dao :as users-dao]
            [buddy.hashers :as hashers]))

(defn- make-req [body]
  {:body body})

(def ^:private mock-user-name "Elochka")

(def ^:private mock-user
  {:id 1 :name mock-user-name :password-hash "hashed-password"})

(deftest validation-test
  (testing "Missing name"
    (let [resp (sut/login-or-register! (make-req {:password "pass123"}))]
      (is (= 400 (:status resp)))))

  (testing "Empty name"
    (let [resp (sut/login-or-register! (make-req {:name "" :password "pass123"}))]
      (is (= 400 (:status resp)))))

  (testing "Still empty name"
    (let [resp (sut/login-or-register! (make-req {:name "   " :password "pass123"}))]
      (is (= 400 (:status resp)))))

  (testing "Missing password"
    (let [resp (sut/login-or-register! (make-req {:name mock-user-name}))]
      (is (= 400 (:status resp)))))

  (testing "Empty password"
    (let [resp (sut/login-or-register! (make-req {:name mock-user-name :password ""}))]
      (is (= 400 (:status resp)))))

  (testing "Too short password"
    (let [resp (sut/login-or-register! (make-req {:name mock-user-name :password "ab"}))]
      (is (= 400 (:status resp)))))

  (testing "Good enough password"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success true :data mock-user})
                  hashers/check
                  (fn [_ _] true)]
      (let [resp (sut/login-or-register! (make-req {:name mock-user-name :password "abc"}))]
        (is (not= 400 (:status resp)))))))

(deftest login-test
  (testing "Successful log in"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success true :data mock-user})
                  hashers/check
                  (fn [_ _] true)]
      (let [resp (sut/login-or-register! (make-req {:name mock-user-name :password "pass123"}))]
        (is (= 200 (:status resp)))
        (is (= mock-user (get-in resp [:body :user]))))))

  (testing "Wrong password"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success true :data mock-user})
                  hashers/check
                  (fn [_ _] false)]
      (let [resp (sut/login-or-register! (make-req {:name mock-user-name :password "wrong"}))]
        (is (= 401 (:status resp))))))

  (testing "Correct name passed to db"
    (let [captured-name (atom nil)]
      (with-redefs [users-dao/find-user-by-name
                    (fn [name]
                      (reset! captured-name name)
                      {:success true :data mock-user})
                    hashers/check
                    (fn [_ _] true)]
        (sut/login-or-register! (make-req {:name mock-user-name :password "pass123"}))
        (is (= mock-user-name @captured-name))))))

(deftest register-test
  (testing "Successful registration"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success false :error-code :not-found :message "Not found"})
                  users-dao/create-user!
                  (fn [_ _] {:success true :data mock-user})
                  hashers/derive
                  (fn [_] "hashed-password")]
      (let [resp (sut/login-or-register! (make-req {:name "Bob" :password "pass123"}))]
        (is (= 200 (:status resp)))
        (is (= mock-user (get-in resp [:body :user]))))))

  (testing "Bypass database failure"
    (with-redefs [users-dao/find-user-by-name
                  (fn [_] {:success false :error-code :not-found :message "Not found"})
                  users-dao/create-user!
                  (fn [_ _] {:success false :error-code :database-error :message "DB down"})
                  hashers/derive
                  (fn [_] "hashed-password")]
      (let [resp (sut/login-or-register! (make-req {:name "Bob" :password "pass123"}))]
        (is (= 500 (:status resp))))))

  (testing "Hash passed password"
    (let [captured-hash (atom nil)]
      (with-redefs [users-dao/find-user-by-name
                    (fn [_] {:success false :error-code :not-found :message "Not found"})
                    users-dao/create-user!
                    (fn [_ hash]
                      (reset! captured-hash hash)
                      {:success true :data mock-user})
                    hashers/derive
                    (fn [_] "hashed-password")]
        (sut/login-or-register! (make-req {:name "Bob" :password "pass123"}))
        (is (= "hashed-password" @captured-hash))))))