(defproject tupelo "0.9.130"
  :description "Tupelo:  Clojure With A Spoonful of Honey"
  :url "http://github.com/cloojure/tupelo"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"

  :dependencies [
    [cheshire "5.8.1"]
    [clj-time "0.15.1"]
    [clojure-csv/clojure-csv "2.0.2"]
    [com.climate/claypoole "1.1.4"]
    [danlentz/clj-uuid "0.1.7"]
    [enlive "1.1.6"]
    [joda-time/joda-time "2.10.1"]
    [org.clojure/clojure "1.10.0"]
    [org.clojure/core.async "0.4.490"]
    [org.clojure/core.match "0.3.0-alpha4"]
    [org.clojure/data.xml "0.2.0-alpha5"]
    [org.clojure/math.combinatorics "0.1.4"]
    [org.clojure/spec.alpha "0.2.176"]
    [org.clojure/test.check "0.9.0"]
    [org.clojure/tools.reader "1.3.2"]
    [prismatic/schema "1.1.10"]
    [reagent-utils "0.3.2"]
  ]
  :plugins [
            [com.jakemccrary/lein-test-refresh "0.22.0"]
            ]

  :deploy-repositories {"snapshots"    :clojars
                        "releases"     :clojars
                        :sign-releases false}

  :global-vars {*warn-on-reflection*      false }

  :source-paths [  "src/clj"   "src/cljc" ]
  :test-paths   [ "test/clj"  "test/cljc" ]
  :target-path  "target/%s"


  ; need to add the compliled assets to the :clean-targets
  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "out"
                                    :target-path]

  :main ^:skip-aot tupelo.core
  ; :uberjar      {:aot :all}

  ; automatically handle `--add-modules` stuff req'd for Java 9 & Java 10
  :jvm-opts ["-Xms500m" "-Xmx4g"
            ]

)
