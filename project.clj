(defproject nomnom/eternity "1.0.1-SNAPSHOT-1"
  :description "Scheduled function execution, as a Component. With optional error reporting and and lock support."
  :min-lein-version "2.5.0"
  :url "https://github.com/nomnom-insights/nomnom.eternity"
  :deploy-repositories {"clojars" {:sign-releases false
                                   :username :env/clojars_username
                                   :password :env/clojars_password}}

  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2018
            :key "mit"}

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [clj-time "0.15.2"]
                 [overtone/at-at "1.2.0"
                  :exclusions [org.clojure/clojure]]
                 [com.stuartsierra/component "1.0.0"]
                 [nomnom/caliban "1.0.3"]
                 [nomnom/lockjaw "0.1.2"]
                 [org.clojure/tools.logging "1.1.0"]]

  :profiles {:dev {:dependencies [[ch.qos.logback/logback-classic "1.2.7"]]}})
