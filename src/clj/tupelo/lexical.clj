;   Copyright (c) Alan Thompson. All rights reserved.  
;   The use and distribution terms for this software are covered by the Eclipse Public
;   License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the
;   file epl-v10.html at the root of this distribution.  By using this software in any
;   fashion, you are agreeing to be bound by the terms of this license.
;   You must not remove this notice, or any other, from this software.
(ns tupelo.lexical
  "Utils for lexical sorting and searching"
  (:use tupelo.core)
  (:refer-clojure :excludes [compare])
  (:require
    [clojure.set :as set]
    [clojure.data.avl :as avl]
    [schema.core :as s]
    [tupelo.schema :as tsk]
    ))

(def Val tsk/Vec)
(def Set (class (avl/sorted-set 1 2 3)))
(def Map (class (avl/sorted-map :a 1 :b 2 :c 3)))

(def lexical-val tsk/Vec)
(def lexical-val tsk/Vec)

; #todo generalize to allow `nil` as an ultimate lower bound?
(s/defn compare :- s/Int
  "Performs a lexical comparison of 2 sequences, sorting as follows:
      [1]
      [1 :a]
      [1 :b]
      [1 :b 3]
      [2]
      [3]
      [3 :y] "
  [a :- tsk/Vec
   b :- tsk/Vec]
  (cond
    (= a b) 0
    (empty? a) -1
    (empty? b) 1
    :else (let [a0 (xfirst a)
                b0 (xfirst b)]
            (if (= a0 b0)
              (compare (xrest a) (xrest b))
              (compare a0 b0)))))

(defn ->sorted-set :- tsk/Map
  "Converts a set into a lexically-sorted set"
  [some-set :- tsk/Set ]
  (into (avl/sorted-set-by compare) some-set))
; #todo add (->sorted-map <map>)        => (into (sorted-map) <map>)
; #todo add (->sorted-vec <sequential>) => (vec (sort <vec>))

(s/defn bound-lower :- tsk/Vec
  "Given a lexical value as a vector such as [1 :a], returns a lower bound like [1]"
  [val :- tsk/Vec]
  (when (zero? (count val))
    (throw (ex-info "Cannot find lower bound for empty vec" {:val val})))
  (xbutlast val))

(s/defn prefix-match? s/Bool
  "Returns true if the sample value equals the pattern when truncated to the same length"
  [pattern :- Val
   sample :- Val]
  (= pattern (xtake (count pattern) sample)))

(s/defn split-key
  "Given a lexically sorted set with like
    #{[:a 1]
      [:a 2]
      [:a 3]
      [:b 1]
      [:b 2]
      [:b 3]
      [:c 1]
      [:c 2]}
   split by partial matche for patterns like [:b] returning a map of 3 sorted sets like:
  {:smaller #{[:a 1]
              [:a 2]
              [:a 3]}
   :matches #{[:b 1]
              [:b 2]
              [:b 3]}
   :larger  #{[:c 1]
              [:c 2]} ]
      "
  [tgt-val :- val
   lexical-set :- Set]
  (let [
        match-val (bound-lower tgt-val)
        [smaller-set -nil- data] (avl/split-key match-val)
        >>        (assert nil? -nil-)
        [matches-seq larger-seq] (split-with prefix-match? data)
        result    {:smaller smaller-set
                   :matches (->sorted-set matches-seq)
                   :larger  (->sorted-set larger-seq)}]
    result))














