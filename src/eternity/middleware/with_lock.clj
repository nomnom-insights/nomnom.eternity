(ns eternity.middleware.with-lock
  (:require
    [clojure.tools.logging :as log]
    [lockjaw.protocol :as lock]))


(defn handler [scheduled-fn]
  (fn [{:keys [lock] :as component}]
    (if (lock/acquire! lock)
      (do
        (log/debugf "lock-status=acquired name=%s" (:name lock))
        (scheduled-fn component))
      (log/debugf "lock-status=none name=%s" (:name lock)))))
