(ns clj-mptt.util
  (:require [clojure.zip :refer [node]]))

(defn data-node? [loc]
  "Location exists and the node is not a vector"
  (if-not (nil? loc)
    (and (not (vector? (node loc)))
         (node loc))))
