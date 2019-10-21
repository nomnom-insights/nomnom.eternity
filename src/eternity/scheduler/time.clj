(ns eternity.scheduler.time
  (:require [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.string :as string]))

(defn to-int [s]
  (Integer. ^String s))

(defn to-interval
  "Convert string indicating time duration
   to time interval in ms
   (to-interval '1s') => 60
   (to-interval '2m') => 120000
   (to-interval '2h') => 3600000
   Supports seconds (s), minutes (m), hours (h) and days (d).
   Cannot combine (e.g '1h1m' not allowed)!!!"
  [t]
  (if (string? t)
    (let [unit (last t)
          v (Integer. ^String (apply str (drop-last t)))]
      (case unit
        \s (* v 1000)
        \m (* v 60 1000)
        \h (* v 60 60 1000)
        \d (* v 24 60 60 1000)))
    t))

(defn- get-closest-minute
  "Given 'mm' return the closest time for mm in future
   e.g now 15:46 and mm=30 => 16:30
       now 15:26 and mm=30 => 15:30"
  [mm now]
  (let [scheduled (time/today-at (time/hour now) (to-int mm))]
    (if (= 1 (compare scheduled now))
      ;; scheduled start is in future
      scheduled
      (time/plus scheduled (time/hours 1)))))

(defn- get-closest-datetime
  "Given 'hh:mm' return the closest time for mm in future
   e.g now 15:46 and hh:mm=15:30 => tomorrow 15:30
       now 15:26 and  hh:mm=15:30 => today 15:30"
  [hh mm now]
  (let [scheduled (time/today-at (to-int hh) (to-int mm))]
    (if (= 1 (compare scheduled now))
      ;; scheduled start is in future
      scheduled
      (time/plus scheduled (time/days 1)))))

(defn calculate-delay
  "Return the number of miliseconds till
   nearest start-at time (can be 'mm' or 'hh:mm').
   Depends on current time whether we need to adjust the hour/day"
  [delay]
  (let [[v1 v2] (string/split delay #":")
        now (time/now)
        start-time (if v2
                     (get-closest-datetime v1 v2 now)
                     (get-closest-minute v1 now))]
    (max 0 (- (coerce/to-long start-time) (coerce/to-long now)))))

(defn get-delay
  "Return delay before scheduler should start"
  [delay]
  (cond
    (integer? delay) delay
    (string? delay) (calculate-delay delay)
    :else
    0))
