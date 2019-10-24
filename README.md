# Eternity

[![Clojars Project](https://img.shields.io/clojars/v/nomnom/eternity.svg)](https://clojars.org/nomnom/eternity)

[![CircleCI](https://circleci.com/gh/nomnom-insights/nomnom.eternity.svg?style=svg)](https://circleci.com/gh/nomnom-insights/nomnom.eternity)

<img src="https://i.annihil.us/u/prod/marvel//universe3zx/images/c/c5/Eternitystand.jpg" align=right heigth="250px" >


Library for scheduling jobs which is suitable to be used in component systems. Wrapper around [at-at](https://github.com/overtone/at-at) which is a wrapper around `java.util.concurrent.ScheduledThreadPoolExecutor`

## Description

There are two components that you will need to setup in your system:

- `scheduler-pool` the `j.u.c.ScheduledThreadPoolExecutor` instance, can (should!) be shared among multiple scheduled jobs
- your scheduled job component(s)

The scheduled job component:
  - requires the scheduler pool as a dependency, by default it uses the `:scheduler-pool` key
  - supported config options:
     - `name`: just to keep track of things
     - `frequency`: how often should scheduler run the function, can be specified in ms (100) or as date string "1m"/"2h"
     - `delay` (optional): used for fine-grained control of when to run the *first time*, can either be specifed in ms (1000) or as time string specifying when scheduler should start
       ("07:00" - at 7am, "30" - at next half hour). Use this option along with `frequency` setting to have schedulers run a function every 4 hours, exactly 15 past the hour
  - passed-in scheduled function: will receive 1 argument: the map of components it depends on

## How to use it

Add component to your system map.

```clojure
(require '[eternity.scheduler :as scheduler]
         '[eternity.pool :as pool])

(def system-map
  {:scheduler-pool (pool/create)
   :every-4h-stuff (component/using
                    (scheduler/create
                     ;; the config: will run every 4hrs, at the beginning of the hour
                     {:name "scheduler-1" :delay "00" :frequency "4h"}
                     ;; scheduled function
                     (fn [components]
                       (do-stuff (:db components)))
                     [:scheduler-pool :db]))
   :every-15s (component/using
                    (scheduler/create
                     ;; the config: will run every 15s, right from the start
                     {:name "scheduler-2" :frequency "15s"}
                     ;; scheduled function
                     (fn [components]
                       (do-very-frequesnt-stuff components))
                     [:scheduler-pool :redis :publisher]))})
```

Following setup will run scheduled job every 4 hours starting at the beginning of the hour.
 On each tick function `do-stuff` will be evaluated where `db` will be passed as arg.

Second scheduled function will run every 15s, as soon as the system starts, the `:redis` and `:publisher` components will be passed in as the dependency map.

### Middlewares

Eternity ships with middlewares, which are just functions wrapping execution of the scheduled function. They're inspired by Ring and you can add yours as long as they do the following:

- accept the scheduled function as the argument
- return a function which accepts  `components` map as an argument
- call the scheduled functions with the components as the argument

This way you can compose many middlewares by nesting function calls. Just like Ring.

Here's a sample/simple logging middleware:

```clojure
(defn with-logging [scheduled-fn]
  (fn [component]
    (log/info "about to do work")
    (scheduled-fn component)
    (log/info "done!")))
```

You can use it in your Component system like so:

```clojure
{ :scheduled-fn (component/using
                  {:name "a sched" :frequency "15s" }
                  (with-logging (fn [component]  (db/do-query (:db-conn component)))))
                [:db-conn :scheduler-pool]}

```

#### Included Middlewares

#### `with-lock` (`eternity.middleware.with-lock`)

Depends on [Lockjaw](https://github.com/nomnom-insights/nomnom.lockjaw) and Postgres, guarantees that only the lock holder will do the job. Requires a `:lock` component as a dependency.
Example:

```clojure

{:scheduler-pool (eternity.pool/create)
  :db-conn (db-pool/create pg-config)
  :lock (component/using
         (lockjaw.create {:name "a-service-name"})
         [:db-conn])
 :scheduled-thing (component/using
                   (eternity.scheduler
                    {:name "scheduled-thing" :frequency "8h"}
                    (eternity.middleware.with-lock/handler
                     ;; will execute only if the lock is acquired!
                     (fn [component]
                       (do-stuff component))))
                   [:lock :scheduler-pool])}


```

#### `with-exception-tracker` (`eternity.middleware.with-exception-tracker`)

Depends on [Caliban](https://github.com/nomnom-insights/nomnom.caliban). Requires `:exception-tracker` component as a dependency.


```clojure

{:scheduler-pool (eternity.pool/create)
  :exception-trakcer (caliban.tracker/create config)
 :scheduled-thing (component/using
                   (eternity.scheduler
                    {:name "scheduled-thing" :frequency "8h"}
                    (eternity.middleware.with-exception-tracker/handler
                     ;; if do -stuff throws an exception it will be logged and reported
                     (fn [component]
                       (do-stuff component))))
                   [:exception-tracker :scheduler-pool])}


```


### Combining middlewares

Just like in Ring, middlewares can be combined:

```clojure

{:scheduler-pool (eternity.pool/create)
 :exception-trakcer (caliban.tracker/create config)
 :lock (component/using (lockjaw.create {:name "service"}) [:db-conn])
 :scheduled-thing (component/using
                   (eternity.scheduler
                    {:name "scheduled-thing" :frequency "8h"}
                    ;; Stack middlewares:
                    (eternity.middleware.with-exception-tracker/handler
                     (eternity.middleware.with-lock/handler
                      (fn [component]
                        (do-stuff component)))))
                   [:exception-tracker :lock :scheduler-pool])}


```


## Roadmap


- [ ] *Maybe* remove direct dependency on `at-at` and use the Scheduled ThreadPool Executor directly
- [ ] More middlewares

# Authors

<sup>In alphabetical order</sup>

- [Afonso Tsukamoto](https://github.com/AfonsoTsukamoto)
- [Łukasz Korecki](https://github.com/lukaszkorecki)
- [Marketa Adamova](https://github.com/MarketaAdamova)
