(ns eternity.scheduler-test
  (:require
    [caliban.tracker.protocol]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [eternity.middleware.with-exception-tracking :as with-exception-tracking]
    [eternity.middleware.with-lock :as with-lock]
    [eternity.pool :as pool]
    [eternity.scheduler :as scheduler]
    [lockjaw.mock]))


;; cant use Caliban mock, as we want to inspect the exception thrown
(defrecord FakeTracker [store]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)
  caliban.tracker.protocol/ExceptionTracker
  (report [_this err]
    (log/error "REPORTING")
    (reset! store err)))


(defrecord test-component [atom]
  component/Lifecycle
  (start [c]
    (reset! atom 0)
    c)
  (stop [c]
    (reset! atom nil)
    c))


(def test-atom (atom 0))


(defn simple-handler [component]
  (log/info "a simple handler")
  (let [test-atom (-> component :test-atom)]
    (swap! test-atom inc)))


(defn exploding-handler [_]
  (log/info "exploding handler")
  (throw (ex-info "FAIL" {})))


(defn test-system [{:keys [handler always-acquire]}]
  (component/map->SystemMap
    {:test-atom test-atom
     :lock (lockjaw.mock/create {:always-acquire always-acquire})
     :exception-tracker (->FakeTracker (atom  nil))
     :scheduler-pool (pool/create)
     :scheduler (component/using
                  (scheduler/create
                    {:name         "test"
                     :frequency     100
                     :delay 100}
                    handler)
                  [:test-atom :lock :exception-tracker :scheduler-pool])}))


(use-fixtures :each (fn [test-fn]
                      (reset! test-atom 0)
                      (test-fn)))


(deftest scheduler-test
  (testing "schedule fn with component"
    (is (= @test-atom 0))
    (let [system (component/start-system (test-system {:handler simple-handler}))]
      ;; initial delay
      (is (= @test-atom 0))
      (Thread/sleep 102)
      (is (= @test-atom 1))
      (Thread/sleep 100)
      (is (= @test-atom 2))
      (component/stop-system system)
      (Thread/sleep 100)
      (is (= @test-atom 2)))))


(deftest lock-middleware
  (testing "has lock and does work"
    (is (= @test-atom 0))
    (let [system (component/start-system (test-system {:handler (with-lock/handler simple-handler)
                                                       :always-acquire true}))]
      (is (= @test-atom 0))
      (Thread/sleep 102)
      (is (= @test-atom 1))
      (Thread/sleep 100)
      (is (= @test-atom 2))
      (component/stop-system system)
      (Thread/sleep 100)
      (is (= @test-atom 2))))
  (testing "doesnt have a lock and doesnt work"
    (reset! test-atom 0)
    (let [system (component/start-system (test-system {:handler (with-lock/handler simple-handler)
                                                       :always-acquire false}))]
      (is (= @test-atom 0))
      (Thread/sleep 102)
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (is (= @test-atom 0))
      (component/stop-system system)
      (Thread/sleep 100)
      (is (= @test-atom 0)))))


(deftest exception-middleware
  (testing "reports exception on handler error"
    (is (= @test-atom 0))
    (let [system (component/start-system (test-system {:handler (with-exception-tracking/handler
                                                                  exploding-handler)}))]
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (testing "verify that exception was reported after throwing"
        (is (= "FAIL"
               (.getMessage (-> system :exception-tracker :store deref)))))
      (component/stop-system system)
      (Thread/sleep 100)
      (testing "verify that no work has been done"
        (is (= @test-atom 0))))))


(deftest both-middlewares
  (testing "uses locks and never runs and throws but that doesnt do anything"
    (is (= @test-atom 0))
    (let [system (component/start-system (test-system {:handler (with-exception-tracking/handler
                                                                  (with-lock/handler
                                                                    exploding-handler))
                                                       :always-acquire false}))]
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (testing "verify that no exception was reported because the handler didn't run"
        (is (nil? (-> system :exception-tracker :store deref)))
        (component/stop-system system)
        (Thread/sleep 100)
        (is (= @test-atom 0)))))
  (testing "uses locks, always runs but throws an exception"
    (is (= @test-atom 0))
    (let [system (component/start-system (test-system {:handler (with-exception-tracking/handler
                                                                  (with-lock/handler
                                                                    exploding-handler))
                                                       :always-acquire true}))]
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (is (= @test-atom 0))
      (Thread/sleep 100)
      (testing "verify that work was done (because lock was acquired) but handler errord and exception was reported"
        (is (= "FAIL" (.getMessage (-> system :exception-tracker :store deref))))
        (component/stop-system system)
        (Thread/sleep 100)
        (is (= @test-atom 0))))))
