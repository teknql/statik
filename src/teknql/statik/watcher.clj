(ns teknql.statik.watcher
  "Polling (GaalVM compatable) file-watcher"
  (:require [clojure.java.io :as io]))

(defn- last-modified-at
  "Returns the last modfied time for dirs-or-files"
  [dirs-or-files]
  (let [all-files (mapcat file-seq dirs-or-files)]
    (apply max (map #(.lastModified ^java.io.File %) all-files))))

(defn watch
  "Watches all files and calls `f` when any of them change.

  Returns a function that will stop the watcher when called."
  [poll-interval dirs-or-files f]
  (let [dirs-or-files (map io/file dirs-or-files)
        quit-atom     (atom false)
        thread
        (Thread. (fn []
                   (loop [last-modified (last-modified-at dirs-or-files)]
                     (Thread/sleep poll-interval)
                     (let [new-last-modified (last-modified-at dirs-or-files)]
                       (when-not (= last-modified new-last-modified)
                         (f))
                       (when-not @quit-atom
                         (recur new-last-modified))))))]
    (.start thread)
    #(reset! quit-atom true)))


(comment
  (watch 100 ["out/"] #(println "Yay"))
  (*1))
