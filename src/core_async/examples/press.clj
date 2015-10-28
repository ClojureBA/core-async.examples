(ns core-async.examples.press
  (:require [clojure.core.async
             :as async
             :refer [<!! >!! <! >!
                     alts! chan go-loop]]
            [clojure.repl :refer [source doc]]
            [clojure.string :as str]
            [clojure.set :refer [intersection]]))

(def publishers [:la-nacion :pagina-12 :new-york-times
                 :clarin :natgeo :the-economist])

(def titles ["Title 1" "Title 2" "Title 3" "Title 4" "Title 5"])

(def topics [:politics :economy :nature :sports :movies
             :entertainment])

(defn publication
  [publisher title topic]
  {:publisher publisher
   :title title
   :topic topic})

(defn rand-publication []
  (apply publication
         (map rand-nth [publishers titles topics])))

(defn create-press []
  (let [ch (chan 10)]
    {:chan ch
     :pub (async/pub ch :topic)}))

(defn read-about
  [press topics]
  (let [chans (repeatedly (count topics) #(chan 1))]
    (mapv #(async/sub (:pub press) %1 %2) topics chans)
    (go-loop []
      (when-let [[val _] (async/alts! chans)]
        (println "\n===> Reading:" val "\n\n")
        (recur)))))

(defn generate-publications [press]
  (let [in (chan)]
    (go-loop []
      (let [p      (rand-publication)
            [_ ch] (alts! [in (async/timeout 1000)])]
        (println "Fresh from the press" p)
        (>! (:chan press) p)
        (when-not (= ch in)
          (recur))))
    in))

(comment

  (def press (create-press))
  (def printing (generate-publications press))

  (read-about press #{:politics})
  (read-about press #{:economy :sports})
  (read-about press #{:entertainment})

  (>!! printing :stop)

  )
