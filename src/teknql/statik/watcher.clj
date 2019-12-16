(ns teknql.statik.watcher
  "Polling (GaalVM compatable) file-watcher"
  (:require [clojure.java.io :as io])
  (:import [java.nio.file ClosedWatchServiceException Path FileSystems
            StandardWatchEventKinds StandardWatchEventKinds$StdWatchEventKind]))

(defn- last-modified-at
  "Returns the last modfied time for dirs-or-files"
  [dirs-or-files]
  (let [all-files (mapcat file-seq dirs-or-files)]
    (apply max (map #(.lastModified ^java.io.File %) all-files))))

(defn poll-watch
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

(defn watch-service-watch
  "Watches all files and calls `f` when any of them change.

  Returns a function that will stop the watcher when called."
  [dirs-or-files f]
  (let [fs              (FileSystems/getDefault)
        watch-service   (.newWatchService fs)
        files           (into #{} (map (fn [file]
                                         (let [file (io/as-file file)
                                               path (.toPath file)]
                                           (if (.isDirectory file)
                                             path
                                             (.getParent path))
                                           )))
                              dirs-or-files)
        register-watch! #(.register ^Path % watch-service
                                    (into-array
                                     StandardWatchEventKinds$StdWatchEventKind
                                     [StandardWatchEventKinds/ENTRY_CREATE
                                      StandardWatchEventKinds/ENTRY_MODIFY]))
        quit-atom       (atom false)
        thread          (Thread. (fn []
                                   (try
                                     (loop [watch-key (.take watch-service)]
                                       (f)
                                       (.pollEvents watch-key)
                                       (.reset watch-key)
                                       (when-not @quit-atom
                                         (recur (.take watch-service))))
                                     (catch ClosedWatchServiceException _
                                       nil))))]
    (doseq [file files]
      (register-watch! file))
    (.start thread)
    #(do (reset! quit-atom true)
         (.close watch-service))))

(defn watch
  "Watches the provided directories or files and calls `f` when any of them change.

  Returns a function that will stop the watcher.

  Reverts to a polling watcher (with 100ms) poll time on MacOS"
  [dirs-or-files f]
  (if (re-find #"(?i)mac|darwin" (System/getProperty "os.name"))
    (poll-watch 100 dirs-or-files f)
    (watch-service-watch dirs-or-files f)))

(comment
  (def quit
    (poll-watch 100 ["out/"] #(println "Yay")))
  (def quit
    (watch-service-watch ["out/"] #(println "Yay")))
  (quit))
