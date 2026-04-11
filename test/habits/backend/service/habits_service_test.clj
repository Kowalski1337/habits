(ns habits.backend.service.habits-service-test
  (:require [clojure.test :refer [deftest is testing]]
            [habits.backend.service.habits-service :as sut]))

(defn- make-req
  ([user-id] (make-req user-id {}))
  ([user-id body]
   {:headers {"user-id" (str user-id)}
    :body    body}))

(deftest get-user-habits-test
  (testing "Get valid"
    (with-redefs [habits.backend.dao.habits-dao/get-user-habits
                  (fn [_] {:success true :data [{:id 1 :title "Run"}]})]
      (let [resp (sut/get-user-habits (make-req 42))]
        (is (= 200 (:status resp)))
        (is (= [{:id 1 :title "Run"}] (:body resp))))))

  (testing "Missing user id"
    (let [resp (sut/get-user-habits {:headers {}})]
      (is (= 400 (:status resp)))))

  (testing "User id wrong format"
    (let [resp (sut/get-user-habits {:headers {"user-id" "abc"}})]
      (is (= 400 (:status resp)))))

  (testing "Bypass db failure"
    (with-redefs [habits.backend.dao.habits-dao/get-user-habits
                  (fn [_] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (sut/get-user-habits (make-req 42))]
        (is (= 500 (:status resp)))))))

(deftest create-habit-test
  (testing "Create valid"
    (with-redefs [habits.backend.dao.habits-dao/create-habit!
                  (fn [_ title _ _] {:success true :data {:id 1 :title title}})]
      (let [resp (sut/create-habit! (make-req 42 {:title "Run" :color "#fff"}))]
        (is (= 201 (:status resp)))
        (is (= "Run" (:title (:body resp)))))))

  (testing "Title is missing"
    (let [resp (sut/create-habit! (make-req 42 {:title ""}))]
      (is (= 400 (:status resp)))))

  (testing "Title is nil"
    (let [resp (sut/create-habit! (make-req 42 {}))]
      (is (= 400 (:status resp)))))

  (testing "Title is empty"
    (let [resp (sut/create-habit! (make-req 42 {:title "   "}))]
      (is (= 400 (:status resp)))))

  (testing "Invalid user id"
    (let [resp (sut/create-habit! {:headers {"user-id" "bad"} :body {:title "Run"}})]
      (is (= 400 (:status resp))))))

(deftest update-habit-test
  (testing "Update title"
    (with-redefs [habits.backend.dao.habits-dao/update-habit!
                  (fn [_ title _ _ _] {:success true :data {:id 1 :title title}})]
      (let [resp (sut/update-habit! (make-req 42 {:title "New title"}) "1")]
        (is (= 200 (:status resp))))))

  (testing "Habit id wrong format"
    (let [resp (sut/update-habit! (make-req 42 {:title "X"}) "abc")]
      (is (= 400 (:status resp)))))

  (testing "Empty update"
    (let [resp (sut/update-habit! (make-req 42 {}) "1")]
      (is (= 400 (:status resp)))))

  (testing "Not found"
    (with-redefs [habits.backend.dao.habits-dao/update-habit!
                  (fn [& _] {:success false :error-code :not-found
                             :message "Not found" :details {:id 1}})]
      (let [resp (sut/update-habit! (make-req 42 {:title "X"}) "1")]
        (is (= 404 (:status resp)))))))

(deftest delete-habit-test
  (testing "Delete correct"
    (with-redefs [habits.backend.dao.habits-dao/delete-habit!
                  (fn [_] {:success true :data {:id 1 :deleted true}})]
      (let [resp (sut/delete-habit! (make-req 42) "1")]
        (is (= 204 (:status resp))))))

  (testing "Not found"
    (with-redefs [habits.backend.dao.habits-dao/delete-habit!
                  (fn [_] {:success false :error-code :not-found
                           :message "Not found" :details {:id 1}})]
      (let [resp (sut/delete-habit! (make-req 42) "1")]
        (is (= 404 (:status resp))))))

  (testing "Habit id wrong format"
    (let [resp (sut/delete-habit! (make-req 42) "abc")]
      (is (= 400 (:status resp))))))