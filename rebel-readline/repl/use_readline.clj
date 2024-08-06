(ns use-readline
  (:require rebel-readline.main
            [clojure.reflect :as reflect :refer [reflect]]
            [cljfmt.core :as fmt]
            [rebel-readline.jline-api :as j])
  (:import [org.jline.reader.impl DefaultParser]
           [org.jline.reader.impl LineReaderImpl]
           [org.jline.terminal Terminal]
           [org.jline.terminal TerminalBuilder]
           [org.jline.terminal.impl DumbTerminal]))

; https://www.infoworld.com/article/3697654/interactive-java-consoles-with-jline-and-consoleui.html?page=2
;; after this ctrl-d exits this line-reader
(rebel-readline.main/-main)
;; Note: with no args this just calls:
;; (rebel-readline.clojure.main/-main)
;; Which in turn just calls
;; (core/ensure-terminal (repl))) ; in rebel-readline.clojure.main
;; and if rebel-readline.jline-api/*terminal*  is already bound it is just
;; rebel-readline.clojure.main/repl
;; which just calls rebel-readline.clojure.main/repl*


;; proxy
;; TODO: wish I could get this to work with a clojure line-reader
;;       this does work. try to get this working for dev purposes
(let [p (proxy [org.jline.reader.impl.LineReaderImpl] [terminal, "p-proxy" {}]
               (readLine [] (str "tiny " (.getAppName this))))]
     (.readLine p))

;; TODO: work on testing framework
;; this works
(binding [*in* (java.io.PushbackReader. (io/reader (.getBytes "hi")))]
    (read *in*))

#_ ;; but this does not
(binding [*in* (java.io.PushbackReader. (io/reader (.getBytes "hello there")))]
  (.readLine line-reader))
;;;;;;;;;;;;;;;;;;;;;Widget copy functions
(defmacro create-widget [& body]
  `(fn [line-reader#]
     (reify Widget
       (apply [_#]
         (widget-exec line-reader# (fn [] ~@body))))))

(defn register-widget [widget-id widget]
  (doto *line-reader*
    (-> (.getWidgets)
        (.put widget-id (if (fn? widget) (widget *line-reader*) widget)))))


(defn widget-exec
"used inside create-widget macro"
[line-reader thunk]

  (binding [*line-reader* line-reader
            *buffer* (.getBuffer line-reader)]
    (try
      (thunk)
      (catch clojure.lang.ExceptionInfo e
        (if-let [message (.getMessage e)]
          (do (log :widget-execution-error (Throwable->map e))
              (display-message
               (AttributedString.
                message (.foreground AttributedStyle/DEFAULT AttributedStyle/RED)))
              true)
          (throw e))))))

(defn call-widget [widget-name]
  (.callWidget *line-reader* widget-name))
;;;;;;;;;;;;;;;;;;;;;Widget copy functions



(def ap (:autopair-widgets @line-reader))
(def ap-can-skip-field
  (doto
    (.getDeclaredMethod (class ap) "canSkip" (into-array [java.lang.String]))
    (.setAccessible true)))

(comment
  ;; TODO invoke not working
  (.invoke ap-can-skip-field ap (into-array Object ["hi"]))
  (def a (into-array ["hi"]))

  (def lr (LineReaderImpl. j/*terminal*))
  (def ap (proxy [AutopairWidgets] [lr true]
            ))

  ;; can't get .invoke to work
  ;; works for 0 arity methods only
  ;; works: (invoke-private-method "SomeLongStringToSubsequence" "subSequence" (int 4) (int 14))
  ;; works: (invoke-private-method "someThing" "equalsIgnoreCase" "Something")
  ;; see j/invoke-private-method and j/get-private-field
  )
