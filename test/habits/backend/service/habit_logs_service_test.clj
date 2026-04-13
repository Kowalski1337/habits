(ns habits.backend.service.habit-logs-service-test
  (:require [clojure.test :refer [deftest is testing]]
            [habits.backend.service.habit-logs-service :as sut]))

(defn- make-req
  ([user-id] (make-req user-id {}))
  ([user-id body] (make-req user-id body {}))
  ([user-id body query-params]
   {:headers      {"user-id" (str user-id)}
    :body         body
    :query-params query-params}))

(def ^:private mock-log
  {:id 1 :habit-id 1 :date "2026-04-13" :completed true :emotion-color "#10B981"})

(deftest get-logs-for-month-test
  (testing "Get valid"
    (with-redefs [habits.backend.dao.habit-logs-dao/get-logs-for-month
                  (fn [& _] {:success true :data [mock-log]})]
      (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "4"}))]
        (is (= 200 (:status resp)))
        (is (= [mock-log] (:body resp))))))

  (testing "Empty logs"
    (with-redefs [habits.backend.dao.habit-logs-dao/get-logs-for-month
                  (fn [& _] {:success true :data []})]
      (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "4"}))]
        (is (= 200 (:status resp)))
        (is (= [] (:body resp))))))

  (testing "User id is missing"
    (let [resp (sut/get-logs-for-month {:headers {} :query-params {"year" "2026" "month" "4"}})]
      (is (= 400 (:status resp)))))

  (testing "User id wrong format"
    (let [resp (sut/get-logs-for-month {:headers {"user-id" "abc"} :query-params {"year" "2026" "month" "4"}})]
      (is (= 400 (:status resp)))))

  (testing "Year is missing"
    (let [resp (sut/get-logs-for-month (make-req 1 {} {"month" "4"}))]
      (is (= 400 (:status resp)))))

  (testing "Month is missing"
    (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026"}))]
      (is (= 400 (:status resp)))))

  (testing "Month out of range high"
    (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "13"}))]
      (is (= 400 (:status resp)))))

  (testing "Month out of range low"
    (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "0"}))]
      (is (= 400 (:status resp)))))

  (testing "Month wrong format"
    (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "abc"}))]
      (is (= 400 (:status resp)))))

  (testing "Bypass database failure"
    (with-redefs [habits.backend.dao.habit-logs-dao/get-logs-for-month
                  (fn [& _] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (sut/get-logs-for-month (make-req 1 {} {"year" "2026" "month" "4"}))]
        (is (= 500 (:status resp)))))))

;; ──────────────────────────────────────────
;; upsert-log!
;; ──────────────────────────────────────────

(deftest upsert-log-test
  (testing "Upsert valid"
    (with-redefs [habits.backend.dao.habit-logs-dao/upsert-log!
                  (fn [& _] {:success true :data mock-log})]
      (let [resp (sut/upsert-log! (make-req 1 {:date       "2026-04-13"
                                               :completed  true
                                               :emotion-color "#10B981"}) "1")]
        (is (= 200 (:status resp)))
        (is (= mock-log (:body resp))))))

  (testing "Upsert without emotion color"
    (with-redefs [habits.backend.dao.habit-logs-dao/upsert-log!
                  (fn [& _] {:success true :data (assoc mock-log :emotion-color "#000000")})]
      (let [resp (sut/upsert-log! (make-req 1 {:date      "2026-04-13"
                                               :completed true}) "1")]
        (is (= 200 (:status resp))))))

  (testing "User id is missing"
    (let [resp (sut/upsert-log! {:headers {} :body {:date "2026-04-13" :completed true}} "1")]
      (is (= 400 (:status resp)))))

  (testing "User id wrong format"
    (let [resp (sut/upsert-log! {:headers {"user-id" "abc"}
                                 :body    {:date "2026-04-13" :completed true}} "1")]
      (is (= 400 (:status resp)))))

  (testing "Habit id wrong format"
    (let [resp (sut/upsert-log! (make-req 1 {:date "2026-04-13" :completed true}) "abc")]
      (is (= 400 (:status resp)))))

  (testing "Date is missing"
    (let [resp (sut/upsert-log! (make-req 1 {:completed true}) "1")]
      (is (= 400 (:status resp)))))

  (testing "Completed is missing"
    (let [resp (sut/upsert-log! (make-req 1 {:date "2026-04-13"}) "1")]
      (is (= 400 (:status resp)))))

  (testing "Habit not found or access denied"
    (with-redefs [habits.backend.dao.habit-logs-dao/upsert-log!
                  (fn [& _] {:success false :error-code :not-found
                             :message "Habit not found or access denied"
                             :details {:id 1}})]
      (let [resp (sut/upsert-log! (make-req 1 {:date "2026-04-13" :completed true}) "1")]
        (is (= 404 (:status resp))))))

  (testing "Bypass database failure"
    (with-redefs [habits.backend.dao.habit-logs-dao/upsert-log!
                  (fn [& _] {:success false :error-code :database-error :message "DB down"})]
      (let [resp (sut/upsert-log! (make-req 1 {:date "2026-04-13" :completed true}) "1")]
        (is (= 500 (:status resp)))))))