(ns eternity.scheduler.time-test
  (:require [clojure.test :refer [deftest is testing]]
            [eternity.scheduler.time :as scheduler.time]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(deftest get-delay-test
  (with-redefs [time/now (fn [] (coerce/to-date-time "2016-01-01T12:30:00"))]
    (testing "no specified"
      (is (= 0 (scheduler.time/get-delay {}))))
    (testing "delay"
      (is (= 123 (scheduler.time/get-delay 123))))
    (testing "minutes"
      (is (= (* 10 60 1000) (scheduler.time/get-delay "40")))
      (is (= (* 40 60 1000) (scheduler.time/get-delay "10"))))
    (testing "hours"
      (is (= (* 10 60 1000) (scheduler.time/get-delay "12:40")))
      ;; 24h without 20 mins
      (is (= (* 1420 60 1000) (scheduler.time/get-delay "12:10"))))))

(deftest to-interval-test
  (testing "to-interval"
    (is (= 1 (scheduler.time/to-interval 1)))
    (is (= 10000 (scheduler.time/to-interval "10s")))
    (is (= 60000 (scheduler.time/to-interval "1m")))
    (is (= 3900000 (scheduler.time/to-interval "65m")))
    (is (= 3600000 (scheduler.time/to-interval "1h")))
    (is (= 86400000 (scheduler.time/to-interval "1d")))))
