(defproject mammutdb-ice "0.1.0-SNAPSHOT"
  :description "MammutDB Front"
  :url "http://github.com/mammutdb/mammutdb-ice"
  :license {:name "BSD (2 Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :resource-paths ["resources"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.5.3"]
                 [cats "0.2.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds {:cljs-repl
                       {:source-paths ["src"]
                        :compiler
                        {:output-to "resources/js/main.js"
                         :output-dir "resources/js/out"
                         :optimizations :none
                         :pretty-print true
                         :source-map true}}}})

