(defproject mr "0.1.0-SNAPSHOT"
  :description ""
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [korma "0.3.0-RC5"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [postgresql "9.1-901.jdbc4"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [lib-noir "0.7.9"]
                 [com.draines/postal "1.11.1"]
                 [clj-time "0.6.0"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]
                 [com.cemerick/friend "0.1.5"]]
  :plugins [[lein-ring "0.8.8"]
            [cider/cider-nrepl "0.6.0"]]
  :ring {:handler mr.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :main mr.handler)
