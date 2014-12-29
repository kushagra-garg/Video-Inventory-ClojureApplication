(defproject hello-seesaw "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [seesaw "1.4.2"]
                 [storm/storm-core "0.9.0.1"]
                 [ororo "0.1.0-alpha1"]
                 [clj-time "0.6.0"]]
  :main ^:skip-aot hello-seesaw.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
