(ns clj-mptt.core
  (:require [clojure.zip :refer [down right left node vector-zip up]]
            [clj-mptt.util :refer [data-node?]]
            [com.rpl.specter :as s :refer [ALL MAP-VALS VAL LAST]]
            [clojure.spec.alpha :as spec]
            [clj-mptt.spec]))

(defn- tag-if [ok loc args]
  (let [[mptt i] args]
    (if-let [node (and ok (data-node? loc))]
      (if (get mptt node)
        [(assoc-in mptt [node :mptt/right] i) (inc i)]
        [(assoc mptt node {:mptt/left i}) (inc i)])
      [mptt i])))

(def ldru (juxt left down right up))

(defn- mptt-trampoline
  [mptt i current last]
  (fn []
    (if current
      (let [[left down right up] (ldru current)]
        (if (and down (= left last))

          ;; if we came upon children from the left then immediately recurse down
          #(mptt-trampoline mptt i down current)

          ;; if we are returning from a lower tier or are carrying on to the right:
          ;; in every case tag the last node with it's :right
          ;; then, if we are coming up, tag the node to the left (it doesn't have a :right yet)
          ;; finally tag the current node with its :left
          (let [[mptt i] (->> [mptt i]
                              (tag-if true last)
                              (tag-if (vector? (node current)) left)
                              (tag-if true current))]

            ;; if we can continue going right, do so
            ;; otherwise jump up to the previous level
            (if right
              #(mptt-trampoline mptt i right current)
              #(mptt-trampoline mptt i up current)))))
      mptt)))

(defn mptt-zip
  "Parse a nested vector data structure to MPTT.

  The supplied data must follow the format [:a :b [:c]] where :a and :b are
  children of the root and :c is a child of :b. A node with children is always
  followed by a vector. A vector is never followed by another vector. A vector
  is never the first element in a vector."
  [data]
  {:pre [(spec/valid? :clj-mptt.spec/mptt data)]}
  (trampoline mptt-trampoline {} 0 (vector-zip data) nil))

(defn- shift-nodes
  "Moves :left / :right at and beyond split by offset"
  [mptt split offset]
  (s/transform [MAP-VALS MAP-VALS (partial <= split)] (partial + offset) mptt))

(defn add-node
  "Insert data into mptt with :left of left and right of left + 1. Overlapping
  boundaries will be shifted to keep a valid tree. Inserting left < 0 or left >
  the maximum right bound is disallowed."
  [mptt data left]
  (let [[max min] ((juxt (partial apply max)
                         (partial apply min))
                    (s/select [MAP-VALS MAP-VALS] mptt))]
    (println max min)
    (if (and (<= min left)
             (>= max left))
      (assoc (shift-nodes mptt left 2) data #:mptt{:left left :right (inc left)})
      (throw (IllegalArgumentException. (str "Invalid left bound " left))))))
