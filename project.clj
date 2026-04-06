(defproject habits "0.1.0-SNAPSHOT"
  :description "Habit tracker app"
  :url "https://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories [["clojars" "https://repo.clojars.org/"]]
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [org.clojure/clojurescript "1.11.132"]

                 ;; Web server & routing
                 [ring/ring-core "1.12.2"]
                 [ring/ring-jetty-adapter "1.12.2"]
                 [compojure "1.7.1"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors/ring-cors "0.1.13"]
                 [metosin/reitit "0.7.2"]

                 ;; React
                 [reagent "2.0.1"]

                 ;; JSON processing
                 [cheshire "5.13.0"]

                 ;; Database
                 [org.postgresql/postgresql "42.7.3"]
                 [com.zaxxer/HikariCP "5.1.0"]
                 [com.github.seancorfield/next.jdbc "1.3.894"]
                 [migratus "1.5.6"]

                 ;; Auth
                 [buddy/buddy-sign "3.5.351"]
                 [buddy/buddy-hashers "2.0.167"]

                 ;; HTTP client for backend
                 [clj-http "3.12.3"]

                 ;; Dev tools
                 [cider/cider-nrepl "0.45.0"]
                 [refactor-nrepl "3.10.0"]

                 ;; Logging
                 [org.slf4j/slf4j-simple "2.0.13"]

                 ;; Shadow
                 [thheller/shadow-cljs "3.3.8"]
                 ]

  :plugins [[migratus-lein "0.7.2"]]

  :shadow-cljs {:builds {:frontend {:target :browser
                                    :output-dir "resources/public/js"
                                    :asset-path "/js"
                                    :modules {:main {:entries [habits.frontend.core]}}
                                    :devtools {:http-root "resources/public"
                                               :http-port 8080}}}}

  :main ^:skip-aot habits.backend.core
  :target-path "target/%s"

  :profiles {:dev {:source-paths ["dev" "src"]
                   :dependencies [[binaryage/devtools "1.0.6"]
                                  [ring/ring-mock "0.4.0"]]
                   :repl-options {:init-ns user
                                  :port 7888}}
             :uberjar {:aot :all
                       :main habits.backend.core
                       :omit-source true}}

  :aliases {"dev" ["with-profile" "dev" "run"]
            "reset" ["do" "clean" "compile"]}

  :migratus {:store :database
             :migration-dir "migrations"
             :db {:classname "org.postgresql.Driver"
                  :subprotocol "postgresql"
                  :subname "//localhost:5432/habit_tracker"
                  :user "postgres"
                  :password "postgres"}}
  :repl-options {:init-ns habits.core}
  )
