(defproject mammutdb-ice "0.1.0-SNAPSHOT"
  :description "MammutDB Front"
  :url "http://github.com/mammutdb/mammutdb-ice"
  :license {:name "BSD (2 Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.6.5"]
                 [cats "0.2.0-SNAPSHOT"]
                 [com.facebook/react "0.9.0.4"]
                 [prismatic/schema "0.2.4"]

                 ; server dependencies
                 [compojure "1.1.8"]
                 [metosin/ring-http-response "0.4.0"]
                 [info.sunng/ring-jetty9-adapter "0.6.0" :exclusions [ring/ring-core]]
                 [ring/ring-ssl "0.2.1" :exclusions [ring/ring-core]]
                 [ring/ring-json "0.3.1" :exclusions [ring/ring-core]]
                 [ring/ring-core "1.2.2" :exclusions [javax.servlet/servlet-api]]
                 [ring/ring-servlet "1.2.2" :exclusions [javax.servlet/servlet-api]]]
  ;:ring {:handler mammutdb.ice.server/app}
  :resource-paths ["resources"]
  :target-path "target/%s"
  :plugins [[lein-cljsbuild "1.0.3"]
            ;[lein-ring "0.8.11"]
            [com.cemerick/austin "0.1.4"]]

  :main ^:skip-aot mammutdb.ice.server
  :cljsbuild {:builds {:cljs-repl
                       {:source-paths ["src"]
                        :compiler
                        {:output-to "resources/public/js/main.js"
                         :output-dir "resources/public/js/out"
                         :source-map true
                         ;; :preamble ["react/react.min.js"]
                         ;; :externs ["react/externs/react.js"]
                         ;; :optimizations :whitespace

                         :optimizations :none
                         :pretty-print true
                         }}}})

