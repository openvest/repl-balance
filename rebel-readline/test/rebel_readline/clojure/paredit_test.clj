(ns rebel-readline.clojure.paredit-test
  (:require [rebel-readline.clojure.paredit :as SUT]
            [rewrite-clj.zip :as z]
            [clojure.test :refer :all])
  (:import [org.jline.reader.impl BufferImpl]))


(def s1 "(defn f[x y]\n  (+ x 8))")
;;;;;;;;;0123456789111 111111122222
;;;;;;;;;          012 345678901234

;;;; String Only Tests
(deftest string-row-offsets-test-s1
  (is (= [0 13 24]
         (SUT/str-row-offsets s1))))

(deftest string-cursor-positions-s1
  (let [finder-fn (SUT/str-find-pos* s1)
        positions [{:col 1, :end-col 2, :end-row 1, :row 1}
                   {:col 2, :end-col 3, :end-row 1, :row 1}
                   {:col 3, :end-col 4, :end-row 1, :row 1}
                   {:col 4, :end-col 5, :end-row 1, :row 1}
                   {:col 5, :end-col 6, :end-row 1, :row 1}
                   {:col 6, :end-col 7, :end-row 1, :row 1}
                   {:col 7, :end-col 8, :end-row 1, :row 1}
                   {:col 8, :end-col 9, :end-row 1, :row 1}
                   {:col 9, :end-col 10, :end-row 1, :row 1}
                   {:col 10, :end-col 11, :end-row 1, :row 1}]]
    (is (fn? finder-fn))
    ;; TODO: extend these tests past 10
    (is (= positions
           (map finder-fn (range 10))))
    (is (= positions
           (map (partial SUT/str-find-pos s1) (range 10))))))

;;;; Zipper/Locator Tests
(def z1 (z/of-string s1 {:track-position? true}))

(deftest loc->position*-test
  (let [loc->position (SUT/loc->position* z1)]
    (is (fn? loc->position))
    (is (= {:col 1 :row 1 , :end-row 2, :end-col 11, :cursor 0,  :end-cursor 23}
           (loc->position z1)))
    (is (= {:col 8 :row 1 , :end-row 1, :end-col 13, :cursor 7,  :end-cursor 12} ; position of [x y]
           (loc->position (-> z1 z/next z/next z/next))))))

(deftest find-loc-test
  (is (= "defn"
         (-> (SUT/find-loc z1 1)
             z/node
             str)))
  (is (= "(+ x 8)"
         (-> (SUT/find-loc z1 15)
             z/node
             str)))
  (is (= s1
         (-> (SUT/find-loc z1 0)
             z/node
             str))))

;; kill tests

(deftest kill-test
  (let [buf (doto (BufferImpl.)
              (.write "(defn f[x y]\n  (+ x 8))")
              (.cursor 15))]
    (SUT/kill buf)
    (is (= "(defn f[x y]\n  )"
           (str buf)))))

(deftest kill-end-test
  "if we end on a closing bracket do nothing"
  (let [buf (doto (BufferImpl.)
              (.write "(foo (bar))")
              ;       "0123456789"
              (.cursor 9))]
    (SUT/kill buf)
    (is (= "(foo (bar))"
           (str buf)))))

(deftest ^:wip kill-require-test
  "wierd case of error inside a require"
  (let [buf (doto (BufferImpl.)
              (.write "(require '[rewrite-clj.paredit :as paredit])")
              ;       "0123456789A"
              (.cursor 10))]
    (SUT/kill buf)
    (is (= "(require )"
           (str buf)))))

(deftest ^:wip kill-sym-test
  "wierd case of error inside a sym"
  (let [buf (doto (BufferImpl.)
              (.write "(foo my-symbol)")
              ;       "0123456789A"
              (.cursor 8))]
    (SUT/kill buf)
    (is (= "(foo my-)"
           (str buf)))))

(deftest slurp-forward-test
  (let [cur 2
        buf (doto (BufferImpl.)
              (.write "[[1 2] [3 4] 5]")
              ;       "012
              (.cursor cur))]
    (SUT/slurp-forward buf)
    (is (= "[[1 2 [3 4]] 5]"
           (str buf)))
    (is (= cur (.cursor buf)))))

(deftest slurp-forward-tail-test
  "slurp forward when at the end of a list type node
   (i.e. no locator there)"
  (let [cur 5
        buf (doto (BufferImpl.)
              (.write "[[1 2] [3 4] 5]")
              ;        012345
              (.cursor cur))]
    (SUT/slurp-forward buf)
    (is (= "[[1 2 [3 4]] 5]"
           (str buf)))
    (is (= cur (.cursor buf)))))

(deftest barf-forward-test
  (let [cur 2
        buf (doto (BufferImpl.)
              (.write "[[1 2 [3 4]] 5]")
              (.cursor cur))]
    (SUT/barf-forward buf)
    (is (= "[[1 2] [3 4] 5]"
           (str buf)))
    (is (= cur (.cursor buf)))))

(deftest slurp-backward-test
  (let [cur 9
        buf (doto (BufferImpl.)
              (.write "[[1 2] [3 4] 5]")
              ;;              =><=
              (.cursor cur))]
    (SUT/slurp-backward buf)
    (is (= "[[[1 2] 3 4] 5]"
           (str buf)))))

(deftest barf-backward-test
  (let [buf (doto (BufferImpl.)
              (.write "[[[1 2] 3 4] 5]")
              ;;            =><=
              (.cursor 7))]
    (SUT/barf-backward buf)
    (is (= "[[1 2] [3 4] 5]"
           (str buf)))))

(deftest splice-test
  (let [buf (doto (BufferImpl.)
              (.write "[[1 2] 3]")
              ;;         =><=
              (.cursor 4))]
    (SUT/splice buf)
    (is (= "[1 2 3]"
           (str buf)))))

(deftest splice-at-tail-test
  (let [buf (doto (BufferImpl.)
              (.write "[[1 2] 3]")
              ;;          =><=
              (.cursor 5))]
    (SUT/splice buf)
    (is (= "[1 2 3]"
           (str buf)))))

(deftest split-ok-test
  (let [buf (doto (BufferImpl.)
              (.write "[[1 2] 3]")
              ;;        =><=
              (.cursor 3))]
    (SUT/split buf)
    (is (= "[[1] [2] 3]"
           (str buf)))))

(deftest ^:wip split-not-ok-test
  "this looks nearly identical to the above test
  it is still before the (node-2) but it fails"
  (let [buf (doto (BufferImpl.)
              (.write "[[1 2] 3]")
              ;;         =><=
              (.cursor 4))]
    (SUT/split buf)
    (is (= "[[1] [2] 3]"
           (str buf)))))

(deftest split-at-string-test
  (let [buf (doto (BufferImpl.)
              (.write "[[1 \"some-long-string\"] 3]")
              ;;                =><=
              (.cursor 10))]
    (SUT/split buf)
    (is (= "[[1 \"some-\" \"long-string\"] 3]"
           (str buf)))))

(comment
  ;all cursor positions
  (pprint (let [buf (doto (BufferImpl.)
                      (.write "[[1 2] 3]"))
                s (str buf)]
            (for [cur (range (inc (count s)))]
              [cur (str (doto buf
                          (.clear)
                          (.write s)
                          (.cursor cur)
                          (.write "|")))])))

  )
