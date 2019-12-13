(ns teknql.statik.main
  "Binary entry point namespace for statik"
  (:require [teknql.statik.core :as statik]
            [cli-matic.core :as cli])
  (:gen-class))


(defn compile-file!
  "Compiles the provided file and writes the generated assets to `out-dir`"
  [{:keys [file out-dir]}]
  (let [assets     (statik/eval-string file)
        output-map (statik/compile assets)]
    (statik/write! output-map out-dir)))

(def cli
  {:app {:command     "statik"
         :description "Static site generator for clojure"
         :version     "0.0.1"}
   :global-opts
   [{:option  "out-dir"
     :short   "o"
     :as      "The directory to output the generated assets to"
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
     :runs        compile-file!}]})


(defn -main [& args]
  (cli/run-cmd args cli))

(comment
  (-main))

(comment
  (compile-file! {:file (slurp "example/site.clj")
                  :out-dir "out"}))
