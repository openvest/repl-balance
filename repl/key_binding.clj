(ns key-binding
  (:require [repl-balance.jline-api :as j :refer [*line-reader* create-widget register-widget]]
            [repl-balance.clojure.line-reader]
            [repl-balance.clojure.paredit :as paredit]
            [clojure.pprint :refer [pprint]])
  (:import [org.jline.keymap KeyMap]))
;; when updating keybindings, ::j/keybindings is generic/builtin
;;      and :key-bindings is user level/overrides

;; bind a key to a widget name
;; why doesn't this work
;(swap! j/*line-reader* update-in  [:key-bindings :main]   (fnil conj []) ["U" "clojure-force-accept-line"])
#_(do (swap! j/*line-reader* update-in [:key-bindings :main] #((fnil conj []) %1 (first %2)) ["U" "clojure-force-accept-line"])
      (j/apply-key-bindings!))
;; something funky with swap! calling the proxy swap which does an (apply swap! ...

;; but this does work
#_(do
    (swap! j/*line-reader* update ::j/key-bindings (constantly {:main [["C" "clojure-force-accept-line"]]}))
    (j/apply-key-bindings!))

;; weird and wonky but this works too
(swap! j/*line-reader* update-in [::j/key-bindings :emacs]
       #((fnil conj []) %1 (first %2))
       [(apply str (map char [24 24])) "beginning-of-buffer"])
(j/apply-key-bindings!)


(swap! j/*line-reader* update-in [::j/key-bindings :emacs]
       #((fnil conj []) %1 (first %2))
       [(apply str (map char [24 24])) "paredit-open-round"])
(j/apply-key-bindings!)


;; first create a widget
;; create-widget is a macro that creats a function
;; that function will take a line-reader and
;; create a method (using reify) to execute
(defn paredit-open-round-fn
  ([] (paredit-open-round-fn j/*buffer*))
  ([buf] (doto buf
           (.write "(pe-open) ")
           (.move -2))))

(def paredit-open-round
 (create-widget
   ;(paredit-open-round-fn)
   (paredit/open-round)
   true))
;; register the widget
(register-widget "paredit-open-round" paredit-open-round )

;; bind a key to call the widget
(j/key-binding :emacs "(" "paredit-open-round")
;; apply the bindings
(j/apply-key-bindings!)

;;;;
(comment
  ;;remove :key-bindings
  (swap! j/*line-reader* dissoc ::j/key-bindings)
  ;; show :key-bindings
  (get-in @j/*line-reader* [::j/key-bindings :main])

  ;; terminal doesn't print keys right... but emacs does??
  (->> @j/*line-reader*
       ::j/key-bindings
       :emacs
       (map (fn [[k v]]
              (let [k (KeyMap/display k)]
                [(subs k 1 (dec (count k))) v])))
       (pprint)))

(->> @j/*line-reader*
     ::j/key-bindings
     :emacs
     (map (juxt (comp #(map int %) first) second))
     (pprint))

(-> (.getKeyMaps j/*line-reader*)
    (get "main")
    (.getBoundKeys)
    (->>
           (map (juxt (comp #(let [k (KeyMap/display %)] (subs k 1 (dec (count k)))) key)
                           #(.toString (val %))))
           (remove #(= "beep" (second %))))
    pprint)
;; "^X" == ctrl-x == 24

;; ^X^D is (24 4)
(-> (apply str (map char [24 4]))
    (KeyMap/display)
    print)

;; home is 71 end is 79
;; https://www.lookuptables.com/coding/keyboard-scan-codes

;; make ctl-X ctl-X beginning-of-buffer
(swap! j/*line-reader* update-in  [::j/key-bindings :emacs]
   #((fnil conj []) %1 (first %2))
   [(apply str (map char [24 24])) "beginning-of-buffer"])
(j/apply-key-bindings!)


(let [km (.get (.getKeyMaps line-reader) "emacs")]
     (.readBinding line-reader km)) ;; returns a Reference
;; enter above then hit ctrl-Y
;; shorter version
(->> (j/key-map "emacs")
     (.readBinding *line-reader*))

(let [km (.get (.getKeyMaps line-reader) "main")
      wname (.name (.readBinding line-reader km))
      widget (.get (.getWidgets line-reader) wname)]
     ;; have to get the widget and .apply
     ;; would normally use (.callWidget line-reader wname) but it only works while isCalling is true
     (.apply widget))
; follow with ctrl-Y or just X
(str (.getBuffer line-reader))

;; show all bindings in a keymap
;; maybe forget this as :repl/key-bindings does a better job
(defn show-key-bindings
  "like (show-key-bindings \"emacs\") "
  [map-name]
 (let [km (.get (.getKeyMaps line-reader) map-name)]
   (->> (.getBoundKeys km)
        (map (juxt (comp #(subs % 1 (dec (count %)))
                         #(KeyMap/display %)
                         key)
                   (comp str
                         val)))
        (filter #(not= "beep" (second %)))
        pprint)))
