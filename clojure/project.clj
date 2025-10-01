(defproject aecs2 "0.1.0-SNAPSHOT"
  :description "Automated Error Correction System II - Clojure Core for the Chernobyl Scientific Project."
  :url "https://github.com/your-repo/CSP-Modern"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.13.0"]]

  :main aecs2.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})