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

Creating MPTT from nested data:

```clojure
(require `[clj-mptt.core :refer [mptt-zip]])

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

## License

Copyright © 2018 Matt Cirillo

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
