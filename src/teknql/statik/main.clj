(ns teknql.statik.main
  "Binary entry point namespace for statik"
  (:require [teknql.statik.core :as statik]
            [org.httpkit.server :as http]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [teknql.statik.watch-middleware :refer [wrap-refresh]]
            [teknql.statik.watcher :as watcher]
            [cli-matic.core :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [garden.core]
            [garden.color]
            [garden.units]
            [garden.stylesheet]
            [taoensso.timbre :as log]
            [clojure.java.shell :as sh])
  (:gen-class))


(defn load-classpaths
  "Loads a deps.edn file at the given path, if it exists."
  [deps-path]
  (let [file (io/file deps-path)]
    (when (.exists file)
      (let [content (slurp file)
            cps     (str/split (:out (sh/sh "clojure" "-Spath" "-Sdeps" content))
                               (re-pattern (System/getProperty "path.separator")))]
        cps))))

(defn compile-file!
  "Compiles the provided file and writes the generated assets to `out-dir`"
  [{:keys [file dir deps]}]
  (try
    (log/info "Compiling" (str file "..."))
    (let [assets     (statik/eval-string (slurp file) (load-classpaths deps))
          output-map (statik/compile assets)]
      (statik/write! output-map dir))
    (catch Exception e
      (log/error e "Error compiling file"))))

(defn serve
  "Starts an http server on the proided port"
  [{:keys [dir port block]
    :or   {block true}}]
  (let [dir     (io/as-file dir)
        handler (-> (fn [req]
                      {:status 404
                       :body   "Not Found"})
                    (wrap-file dir)
                    (wrap-content-type)
                    ((fn [handler]
                       (fn [req]
                         (let [uri (:uri req)
                               req (assoc req :uri
                                          (if (str/ends-with? uri "/")
                                            (str uri "index.html")
                                            uri))]
                           (handler req)))))
                    (wrap-refresh [dir]))]
    (log/info "Starting HTTP Server:" (str "http://localhost:" port))
    (let [stop (http/run-server handler {:port port})]
      (if-not block
        stop
        (loop []
          (let [input (read-line)]
            (if (= "quit" input)
              (do (stop)
                  (log/info "Goodbye!"))
              (recur))))))))

(defn watch
  "Starts a development server and watches the provided file for changes"
  [{:keys [dir port block file]
    :or   {block true}}]
  (compile-file! {:dir dir :file file})
  (watcher/watch [file] #(compile-file! {:file file :dir dir}))
  (serve {:dir dir :port port :block block}))


(def cli
  (let [file-opt {:option "file"
                  :as     "The file to ealuate"
                  :type   :string
                  :short  0}
        deps-opt {:option  "deps"
                  :as      "Path to deps.edn file"
                  :type    :string
                  :default "deps.edn"}
        port-opt {:option  "port"
                  :type    :int
                  :as      "The port to start the server on"
                  :default 3000
                  :short   "p"}
        dir-opt  {:option  "dir"
                  :short   "d"
                  :as      "The directory to output to / serve from"
                  :type    :string
                  :default "out"}]
    {:app {:command     "statik"
           :description "Static site generator for clojure"
           :version     "0.0.1"}
     :commands
     [{:command     "compile"
       :description "Compiles the provided file"
       :opts        [file-opt dir-opt deps-opt]
       :runs        compile-file!}
      {:command     "serve"
       :description "Serves the specified directorty"
       :opts        [port-opt deps-opt (assoc dir-opt
                                              :short 0
                                              :default "./")]
       :runs        serve}
      {:command     "watch"
       :description "Watches file for changes and live reloads them"
       :opts        [file-opt port-opt dir-opt deps-opt]
       :runs        watch}]}))


(defn -main [& args]
  (cli/run-cmd args cli))

(comment
  ((-main "compile" "example/src/site.clj")))

(comment
  (compile-file! {:dir  "out"
                  :file "example/site.clj"})

  (watch {:dir   "out"
          :file  "example/site.clj"
          :block false
          :port  4000}))
