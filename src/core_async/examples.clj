(ns core-async.examples
  (:require [clojure.core.async
             :as async
             :refer [<!! >!! <! >!
                     chan sliding-buffer dropping-buffer
                     go chan go-loop]]
            [clojure.repl :refer [source doc]]
            [clojure.string :as str]))

(defn divide [] (println "=============================="))

;; -----------------------------------------------------------
;; CHANNELS & BUFFERS
;; -----------------------------------------------------------

;; Blocking put & take

(defn >!!-n [ch n]
  (divide)
  (dotimes [i n]
    (println "putting >" i)
    (>!! ch i))
  ch)

(defn <!!-n [ch n]
  (dotimes [_ n]
    (println "taking <" (<!! ch)))
  ch)

(comment
  (doc chan)

  {:puts    [,,,] ;; LinkedList
   :buffer  [,,,] ;; Queue
   :takes   [,,,] ;; LinkedList
   ,,,}

  ;; ## Fixed Buffer

  ;; (>!!-n (async/chan) 1)

  (let [ch (chan 1)]
    (>!!-n ch 1)
    (<!!-n ch 1))

  (let [ch (chan 3)]
    (>!!-n ch 3)
    (<!!-n ch 2)
    (<!!-n ch 1))

  (let [ch (chan 42)]
    (>!!-n ch 3)
    (<!!-n ch 2)
    (<!!-n ch 1))

  ;; Can I put any number of vals in a fixed buffered channel?
  ;; Yes and no
  (let [ch (chan 3343)]
    (>!!-n ch 200)
    (<!!-n ch 150))

  ;; ## Sliding buffer (size = 2)
  ;; [_ 0]
  ;;   [0 1]
  ;;     [1 2]
  ;;       [2 3]
  ;;    0 1 2 3 ...
  (let [ch (chan (sliding-buffer 2))]

    (>!!-n ch 3)  ;; [0], [0, 1], [1, 2]
    (<!!-n ch 2)  ;; [1, 2] <- []

    (>!!-n ch 7)  ;; [0], [0, 1], [1, 2], ..., [5, 6]
    (<!!-n ch 1)  ;; 5 <- [6]

    (>!!-n ch 1)  ;; [6, 0]
    (<!!-n ch 1)) ;; 6 <- [0]

  ;; ## Dropping buffer
  ;;  [_ _ _ _ _]
  ;;   0 1 2 3 4 5 6 7 8 ...
  ;;             |-> dropped
  (let [ch (chan (dropping-buffer 5))]

    (>!!-n ch 5)  ;; [0, 1, 2, 3, 4]
    (<!!-n ch 1)  ;; 0 <- [1, 2, 3, 4]

    (>!!-n ch 15) ;; [1, 2, 3, 4, 0]
    (<!!-n ch 2)  ;; [1, 2] <- [3, 4, 0]

    (>!!-n ch 10) ;; [3, 4, 0, 0, 1]
    (<!!-n ch 5)) ;; [3, 4, 0, 0, 1] <- []
  )

;; Go blocks - Parking put & take

(defn echo-once [in]
  (go (println (<! in))))

(defn rand-speaker [out words]
  (go (>! out (rand-nth words))))

(def words (str/split "Everybody dance now! CHAN, chAN, CHan, chan!" #" "))

(comment
  ;; Echo
  (let [tube (chan)]
    (divide)
    (echo-once tube)
    (rand-speaker tube words))
  )

(defn echo-forever [in]
  (go (while true (println (<! in)))))

(comment
  ;; Echo
  (let [tube (chan)]
    (divide)
    (echo-forever tube)
    (rand-speaker tube words)
    (rand-speaker tube words)
    (rand-speaker tube words))
  )

;; Let's speak a whole sentence

(defn speaker [out words]
  (go-loop [[w & ws] words]
    (when w
      (println w)
      (recur ws))))

(comment

  ;; Echo
  (let [tube (chan)]
    (divide)
    (echo-forever tube)
    (speaker tube words))

  )

(comment
  ;; Two go blocks
  (let [hi-chan (chan)]
    (divide)
    (go (dotimes [i 10]
          (>! hi-chan (str "hi " i))))
    (go (dotimes [i 10]
          (println (<! hi-chan)))))

  ;; Let's try this instead (result?)
  ;; 11 go blocks
  (let [hi-chan (chan)]
    (divide)
    (dotimes [i 10]
      (go (>! hi-chan (str "hi " i))))
    (go
      (dotimes [i 10]
        (println (<! hi-chan)))))

  )

;; Peeking into the go macro
(comment

  (clojure.pprint/pprint
   (macroexpand-1 '(go (<! (chan)))))

  )

;; ## Links
;; Core Async Go Macro Internal - Part 1 [https://www.youtube.com/watch?v=R3PZMIwXN_g]
;; Clojure core.async Channels - [http://clojure.com/blog/2013/06/28/clojure-core-async-channels.html]
