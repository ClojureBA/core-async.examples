(ns core-async.examples.wazzup
  (:require [clojure.core.async
             :as async
             :refer [<!! >!! <! >!
                     chan sliding-buffer dropping-buffer
                     go chan go-loop]]
            [clojure.repl :refer [source doc]]
            [clojure.string :as str]))

(defn phone-line []
  (let [ch (chan (sliding-buffer 10))]
    {:chan ch
     :mult (async/mult ch)}))

(defn power [n p]
  (int (java.lang.Math/pow n p)))

(defn wazzup [n]
  (apply str
         "WAZA"
         (apply str (repeat (power n 1.5) "A"))))

(defn send-wazzup [person]
  (async/put! (:out person) [(:name person) (wazzup 0)]))

(def ^:const MAX-WAZZUPS 10)

(defn wazzuper
  [name line]
  (let [xform (filter (fn [[sender _]]
                         ;;(println "[Filtering]" sender (not= name sender))
                         (not= name sender)))
        out   (:chan line)
        in    (chan (dropping-buffer 10) xform)]
    (async/tap (:mult line) in)
    (println [name])
    (go-loop [n 0]
      (let [[sender msg] (<! in)]
        (println  [name] "got" [sender msg])
        (when (and (< n MAX-WAZZUPS) sender)
          (>! out [name (wazzup n)])

          (println [name] (- MAX-WAZZUPS n) "has WAZUPS left")
          (when (< n MAX-WAZZUPS)
            (<! (async/timeout (rand-int 5000))))

          (recur (inc n)))))
    {:in in
     :out out
     :name name}))

(defn wire-tap [line]
  (let [line-tap (chan (dropping-buffer 10))]
    (async/tap (:mult line) line-tap)
    (go-loop []
      (when-let [[speaker msg] (<! line-tap)]
        (println "== [The Wire] ==>" speaker ":" msg)
        (recur)))))

(comment
  (do
    (println "================================================")

    (def line (phone-line))

    (def johnny (wazzuper "Johnny" line))
    (def alicia (wazzuper "Alicia" line))
    (def timmy (wazzuper "Timmy" line))

    (wire-tap line)

    (send-wazzup johnny)
    ;;(say-wazzup alicia)
    ;;(say-wazzup timmy)

    ;; (async/put! (:chan line) [:react johnny])

    )
  )
