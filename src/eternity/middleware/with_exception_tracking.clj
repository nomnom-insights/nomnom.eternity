(ns eternity.middleware.with-exception-tracking
  (:require [clojure.tools.logging :as log]
            [caliban.tracker.protocol :as tracker]))

(defn handler [scheduled-fn]
  (fn [{:keys [exception-tracker] :as component}]
    (try
      (scheduled-fn component)
      (catch Exception err
        (log/error err)
        (tracker/report exception-tracker err)))))
