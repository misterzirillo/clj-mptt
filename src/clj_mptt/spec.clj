(ns clj-mptt.spec
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]
            [clj-mptt.util :refer [data-node?]]))

(s/def ::mptt (s/and vector?
                     (s/+ (s/cat :data (s/+ (comp not vector?))
                                 :vector (s/? ::mptt)))))
