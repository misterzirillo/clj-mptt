# clj-mptt

A set of Clojure utilities for creating and manipulating Modified Preorder Traversal Trees.

## What is Modified Preorder Tree Traversal?
_from https://github.com/django-mptt/django-mptt_

MPTT is a technique for storing hierarchical data in a database. The aim is to
make retrieval operations very efficient.

The trade-off for this efficiency is that performing inserts and moving
items around the tree is more involved, as there's some extra work
required to keep the tree structure in a good state at all times.

Here are a few articles about MPTT to whet your appetite and provide
details about how the technique itself works:

* [Trees in SQL](http://www.ibase.ru/files/articles/programming/dbmstrees/sqltrees.html)
* [Storing Hierarchical Data in a Database](http://www.sitepoint.com/print/hierarchical-data-database)
* [Managing Hierarchical Data in MySQL](http://mikehillyer.com/articles/managing-hierarchical-data-in-mysql/)

## Usage

### Creating MPTT from nested vectors

```clojure
(require `[clj-mptt.core :refer [mptt-zip add-new-node remove-node right-of]])

(mptt-zip [:a :b [:c :d] :e :f [:g :h :i]])
;=>
;{:a #:mptt{:left 0, :right 1},
; :b #:mptt{:left 2, :right 7},
; :c #:mptt{:left 3, :right 4},
; :d #:mptt{:left 5, :right 6},
; :e #:mptt{:left 8, :right 9},
; :f #:mptt{:left 10, :right 17},
; :g #:mptt{:left 11, :right 12},
; :h #:mptt{:left 13, :right 14},
; :i #:mptt{:left 15, :right 16}}
```

The parser understands a vector like the form above. Some rules apply:

1. A vector must not be empty.
2. A vector may not be the first element in a vector.
3. A vector may only follow a non-vector.

Basically each vector describes a level of heirarchy. With the exception of the root vector, each vector describes the children of the element before it. Above you can see that ```:c``` and ```:d``` are children of ```:b```. The data itself can be anything (although its probably wise to use some unique identifier) and the level of nesting is arbitrary.

_Note: The functions of this library output maps but it is easier to understand, visually, if the MPTT is printed in the literal form. The examples below show output in the literal form instead of the actual map output._

### Adding and removing data
These functions modify the MPTT map while keeping the structure valid.

```clojure
(def mptt (mptt-zip [:a :b [:c :d]]))
(add-new-node mptt :e (right-of mptt :b)) ;; => [:a :b [:c :d] :e]
(remove-node mptt :b) ;; => [:a]
```

### Moving nodes around

```clojure
;; move one node
(def mptt (mptt-zip [:a :b [:c :d]]))
(move-node mptt :b (left-of mptt :a)) ;; => [:b [:c :d] :a]

;; move many nodes
(def mptt2 (mptt-zip [:a [:b [:c [:d]]]))
(move-nodes mptt [:b :c :d] (right-of mptt :a)) ;; => [:a :b :c :d]
```

### Targeting helpers
Insertion and move operations take a left-bound parameter to specify where the action should happen. In most operations the boundary with the specified value and any boundaries that are larger will be modified to make space or collapse space for new and removed values.

```clojure
(def mptt (mptt-zip [:a])) ;; => {:a {:left 0 :right 1}}
(add-new-node mptt :b 0) ;; => [:b :a]
(add-new-node mptt :b 1) ;; => [:a [:b]]
(add-new-node mptt :b 2) ;; => [:a :b]
```

This behavior is fairly implementation specific and at first glance isn't very intuitive so there are some helpers to make it easier:

```clojure
;; you were going to write these anyway
(def mptt (mptt-zip [:a])) ;; => {:a {:left 0 :right 1}}
(add-new-node mptt :b (left-of mptt :a)) ;; => [:b :a]
(add-new-node mptt :b (first-child mptt :a)) ;; => [:a [:b]]
(add-new-node mptt :b (last-child mptt :a)) ;; => [:a [:b]]
(add-new-node mptt :b (right-of mptt :a)) ;; => [:a :b]
```

__TODO__ These should just be built in options for each function.

## License

Copyright © 2018 Matt Cirillo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
