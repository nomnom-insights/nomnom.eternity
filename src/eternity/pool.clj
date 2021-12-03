(ns eternity.pool
  (:require
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [overtone.at-at :as at]))


(defrecord SchedulerPool []
  component/Lifecycle
  (start [component]
    (if (:pool component)
      component
      (do
        (log/info "starting pool")
        (assoc component :pool (at/mk-pool)))))

  (stop [component]
    (when-let [pool (:pool component)]
      (let [jobs (at/scheduled-jobs pool)]
        (log/infof "stopping pool jobs:%s" jobs)
        (at/stop-and-reset-pool! pool)))
    (assoc component :pool nil)))


(defn create []
  (SchedulerPool.))
