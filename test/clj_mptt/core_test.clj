(ns clj-mptt.core-test
  (:require [clojure.test :refer :all]
            [clj-mptt.core :refer :all]
            [com.rpl.specter :as s]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(deftest mptt-parser
  (testing "MPTT parser"
    (let [data [:a :b [:c :d] :e]
          mptt (mptt-zip data)]
      (is (= mptt
             {:a #:mptt{:left 0
                       :right 1}
              :b #:mptt{:left 2
                       :right 7}
              :c #:mptt{:left 3
                       :right 4}
              :d #:mptt{:left 5
                       :right 6}
              :e #:mptt{:left 8
                       :right 9}})))))

