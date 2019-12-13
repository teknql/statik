(ns teknql.statik.main
  "Binary entry point namespace for statik"
  (:require [teknql.statik.core :as statik]
            [ring.middleware.file :as rm.file]
            [ring.middleware.content-type :as rm.content-type]
            [ring.middleware.not-modified :as rm.not-modified]
            [ring.adapter.jetty :as jetty]
            [cli-matic.core :as cli]
            [clojure.java.io :as io])
  (:gen-class))


(defn compile-file!
  "Compiles the provided file and writes the generated assets to `out-dir`"
  [{:keys [file dir]}]
  (let [assets     (statik/eval-string file)
        output-map (statik/compile assets)]
    (statik/write! output-map dir)))

(defn serve
  "Starts an http server on the proided port"
  [{:keys [dir port async]}]
  (let [handler (-> (fn [req]
                      (if-some [file (rm.file/file-request req dir)]
                        file
                        {:status 404
                         :body   "Not Found"}))
                    (rm.content-type/wrap-content-type)
                    (rm.not-modified/wrap-not-modified))]
    (println "Starting HTTP Server: http://localhost:" port )
    (jetty/run-jetty handler {:port   port
                              :async? async})))

(def cli
  {:app {:command     "statik"
         :description "Static site generator for clojure"
         :version     "0.0.1"}
   :global-opts
   [{:option  "dir"
     :short   "d"
     :as      "The directory to output to / serve from"
     :type    :string
     :default "out"}]
   :commands
   [{:command     "compile"
     :description "Compiles the provided file"
     :opts
     [{:option "file"
       :as     "The file to ealuate"
       :type   :slurp
       :short  0}]
     :runs        compile-file!}
    {:command     "serve"
     :description "Serves the specified directorty"
     :opts
     [{:option  "port"
       :type    :int
       :as      "The port to start the server on"
       :default 3000}]
     :runs        serve}]})


(defn -main [& args]
  (cli/run-cmd args cli))

(comment
  (-main))

(comment
  (compile-file! {:file (slurp "example/site.clj")
                  :dir  "out"})

  (serve {:dir  "out"
          :port 4000}))
