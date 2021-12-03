(ns eternity.middleware.with-exception-tracking
  (:require
    [caliban.tracker.protocol :as tracker]
    [clojure.tools.logging :as log]))


(defn handler [scheduled-fn]
  (fn [{:keys [exception-tracker] :as component}]
    (try
      (scheduled-fn component)
      (catch Exception err
        (log/error err)
        (tracker/report exception-tracker err)))))
