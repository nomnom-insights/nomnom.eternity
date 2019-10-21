(ns eternity.scheduler
  (:require [eternity.scheduler.time :as time]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as component]))

(defrecord SchedulerJob [name interval initial-delay schedule-fn scheduler-pool]
  component/Lifecycle
  (start [component]
    (if (:job component)
      component
      (let [_ (when-not scheduler-pool
                (throw (ex-info "scheduler-pool is a required dependency" {})))
            handler (fn scheduled [] (schedule-fn component))
            func (at/every interval
                           handler
                           (:pool scheduler-pool)
                           :desc name
                           :initial-delay initial-delay)]
        (log/infof "starting-scheduler name=%s interval=%s delay=%s"
                   name interval initial-delay)
        (assoc component :job func))))

  (stop [component]
    (if-let [job (:job component)]
      (do
        (log/warnf "stoppping-scheduler name=%s" (:desc job))
        (at/stop job)
        (assoc component :job nil))
      component)))

(defn create [{:keys [name frequency delay]}
              schedule-fn]
  {:pre [(string? name)
         (or (string? frequency) (integer? frequency))
         (fn? schedule-fn)]}
  (map->SchedulerJob {:name name
                      :interval (time/to-interval frequency)
                      :schedule-fn schedule-fn
                      :initial-delay (time/get-delay delay)}))
