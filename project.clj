(defproject marshmallow "test-version"
  :description "Tools for marshmallow game"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [dk.ative/docjure "1.11.0"]
                 [instaparse "1.4.7"]]
  :main ^:skip-aot marshmallow.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
