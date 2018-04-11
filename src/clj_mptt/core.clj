(ns clj-mptt.core
  (:require [clojure.zip :refer [down right left node vector-zip up]]
            [com.rpl.specter :as s :refer [ALL MAP-VALS VAL LAST]]
            [clojure.spec.alpha :as spec]
            [clj-mptt.spec]))

;;;;;;;;;;;;;;;
;; UTILITIES ;;
;;;;;;;;;;;;;;;

(def get-left-right (juxt :mptt/left :mptt/right))

(defn is-child?
  [parent child]
  (let [[parent-left parent-right] (get-left-right parent)
        [left right] (get-left-right child)]
    (and (> left parent-left)
         (< right parent-right))))



;;;;;;;;;;;;;
;; PARSING ;;
;;;;;;;;;;;;;
(defn- data-node? [loc]
  "Location exists and the node is not a vector"
  (if-not (nil? loc)
    (and (not (vector? (node loc)))
         (node loc))))

(defn- tag-if [ok loc args]
  (let [[mptt i] args]
    (if-let [node (and ok (data-node? loc))]
      (if (get mptt node)
        [(assoc-in mptt [node :mptt/right] i) (inc i)]
        [(assoc mptt node {:mptt/left i}) (inc i)])
      [mptt i])))

(def ^:private ldru (juxt left down right up))

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

;;;;;;;;;;;;;;;
;; OPERATIONS;;
;;;;;;;;;;;;;;;
(defn- shift-nodes
  "Moves :left / :right at and beyond split by offset"
  [mptt split offset]
  (s/transform [MAP-VALS MAP-VALS (partial <= split)] (partial + offset) mptt))

(defn- last-child
  "Helper to target the last child position of the node for k"
  [mptt k]
  (s/select-one [k :mptt/right] mptt))

(defn- left-of
  "Helper to target the position to the left of the node k"
  [mptt k]
  (s/select-one [k :mptt/left] mptt))

(defn- first-child
  "Helper to target the first child position of the node for k"
  [mptt k]
  (inc (left-of mptt k)))

(defn- right-of
  "Helper to target the position to the right of the node for k"
  [mptt k]
  (inc (last-child mptt k)))

(defn- place
  [mptt locator k]
  (if (k mptt)
    (case locator
      :last-child (last-child mptt k)
      :left-of (left-of mptt k)
      :first-child (first-child mptt k)
      :right-of (right-of mptt k))
    (throw (IllegalArgumentException. (str "Unknown element " k)))))

(defn add-new-node
  "Insert new data into tree with no children with the specified left."
  [mptt data locator k]
  (if-let [left (place mptt locator k)]
    (assoc (shift-nodes mptt left 2) data #:mptt{:left left :right (inc left)})))

(defn get-children
  [mptt k]
  (let [parent (get mptt k)]
    (s/select [(s/filterer [LAST #(is-child? parent %)]) ALL] mptt)))

(defn remove-node
  "Remove node k from mptt. Returns vector [mptt removed] where removed contains
  every removed node (k and k's children)."
  [mptt k]
  (if-let [node (get mptt k)]
    (let [[left right] (get-left-right node)
          children (get-children mptt k)]
      [(-> (apply dissoc mptt (s/select s/MAP-KEYS children))
           (dissoc k)
           (shift-nodes left (dec (- left right))))
       (into (hash-map) (cons [k node] children))])
    (throw (IllegalArgumentException. "Unknown element " k))))

(defn move-node
  "Moves node at k to a different position in mptt with a new :left of new-left.
  new-left is required to be a child position of the root but not a child position of k."
  [mptt k locator t]
  (if-let [new-left (place mptt locator t)]
    (let [[new-tree moved] (remove-node mptt k)             ;; remove the nodes that are moving
          [left right] (get-left-right (get mptt k))
          removed-width (inc (- right left))
          new-left      (if (< right new-left)              ;; find new-left if its position changed
                          (- new-left removed-width)
                          new-left)]

      ;; make space for the incoming nodes in new-tree
      ;; then align moved boundaries with new-tree and merge them
      (-> (shift-nodes new-tree new-left removed-width)
          (merge (shift-nodes moved 0 (- new-left left)))))))

(defn move-nodes
  "Takes nodes specified by ks and moves them in order, to the new location provided the move
  is valid. Each node is placed to the right of the one that was previously moved.

  ;; [:a :b [:c :d [:e]]] mptt
  (move-nodes mptt [:a :c :d] (right-of mptt :b))
  ;; => [:b :a :c :d [:e]]"
  [mptt ks locator k]
  (loop [mptt mptt
         locator locator
         last k
         [k & more] ks]
    (if k
      (recur
        (move-node mptt k locator last)                     ;; actually do the move
        :right-of k                                         ;; the next node will be put to the right of this one
        more)
      mptt)))
