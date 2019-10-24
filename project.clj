(defproject nomnom/eternity "1.0.0"
  :description "Scheduled function execution, as a Component. With optional error reporting and and lock support."
  :min-lein-version "2.5.0"
  :url "https://github.com/nomnom-insights/nomnom.eternity"
  :deploy-repositories {"clojars" {:sign-releases false
                                   :username [:gpg :env/clojars_username]
                                   :password [:gpg :env/clojars_password]}}

  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2018
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-time "0.15.2"]
                 [overtone/at-at "1.2.0"
                  :exclusions [org.clojure/clojure]]]

  :plugins [[lein-cloverage "1.0.13" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[com.stuartsierra/component "0.4.0"]
                                  [nomnom/caliban "1.0.2"]
                                  [nomnom/lockjaw "0.1.2"]
                                  [org.clojure/tools.logging "0.5.0"]
                                  [ch.qos.logback/logback-classic "1.2.3"]]}})
