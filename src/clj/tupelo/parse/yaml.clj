(ns tupelo.parse.yaml
  (:use tupelo.core)
  (:require
    [clojure.walk :as walk]
    [schema.core :as s]
    [tupelo.schema :as tsk]
    )
  (:import
    [org.snakeyaml.engine.v1.api LoadSettingsBuilder Load]
    [org.snakeyaml.engine.v1.api DumpSettingsBuilder Dump]
    ))

;-----------------------------------------------------------------------------
(def ^:private load-settings (-> (LoadSettingsBuilder.)
                               ;(.setLabel "Custom user configuration")
                               (.build)))

(def ^:private yaml-load (Load. load-settings))

(s/defn parse-all-raw
  "Parses a String containing multiple YAML objects, returning a vector of normalized Clojure data structure."
  [str-in :- s/Str]
  (unlazy (.loadAllFromString yaml-load (s/validate s/Str str-in))))

(s/defn parse-all
  "Parses a String containing multiple YAML objects, returning a vector of normalized Clojure data structure (with keywordized map keys)."
  [str-in :- s/Str]
  (walk/keywordize-keys
    (parse-all-raw (s/validate s/Str str-in))))

(s/defn parse-raw
  "Parses a String containing a single YAML object, returning a normalized Clojure data structure."
  [str-in :- s/Str]
  (unlazy (.loadFromString yaml-load (s/validate s/Str str-in))))

(s/defn parse
  "Parses a String containing a single YAML object, returning a normalized Clojure data structure (with keywordized map keys)."
  [str-in :- s/Str]
  (walk/keywordize-keys
    (parse-raw (s/validate s/Str str-in))))

;-----------------------------------------------------------------------------
(def ^:private dump-settings (it-> (DumpSettingsBuilder.)
                               ;(.setDefaultScalarStyle it ScalarStyle.DOUBLE_QUOTED)
                               (.build it)))

(def ^:private yaml-dump (Dump. dump-settings))

(defn encode [it]
  "Serializes a Clojure data structure into a YAML string."
  (.dumpToString yaml-dump
    (walk/stringify-keys it)))

