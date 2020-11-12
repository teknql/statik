(ns teknql.statik.classpath
  "Namespace for building a sci load-fn based off a tools-deps deps.edn map.

  Based heavily on the implementation of Babashka's Classpath loader:

  https://github.com/borkdude/babashka/blob/master/src/babashka/impl/classpath.clj"
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util.jar JarFile]))

(set! *warn-on-reflection* true)

(defprotocol IResourceResolver
  (getResource [this paths]))

(deftype DirectoryResolver [path]
  IResourceResolver
  (getResource [this resource-paths]
    (some
      (fn [resource-path]
        (let [f (io/file path resource-path)]
          (when (.exists f)
            (slurp f))))
      resource-paths)))

(defn path-from-jar
  [^java.io.File jar-file resource-paths]
  (with-open [jar (JarFile. jar-file)]
    (some (fn [path]
            (when-let [entry (.getEntry jar path)]
              (slurp (.getInputStream jar entry))))
          resource-paths)))

(deftype JarFileResolver [jar-file]
  IResourceResolver
  (getResource [this resource-paths]
    (path-from-jar jar-file resource-paths)))

(deftype Loader [entries]
  IResourceResolver
  (getResource [this resource-paths]
    (some #(getResource % resource-paths) entries)))

(defn path->entry [path]
  (if (str/ends-with? path ".jar")
    (JarFileResolver. (io/file path))
    (DirectoryResolver. (io/file path))))

(defn source-for-namespace [loader namespace]
  (let [ns-str         (name namespace)
        ^String ns-str (munge ns-str)
        base-path      (.replace ns-str "." "/")
        resource-paths (mapv #(str base-path %) [".clj"])]
    (getResource loader resource-paths)))

(defn build-loader [paths]
  (let [entries (map path->entry paths)]
    (Loader. entries)))

(defn build-load-fn
  [classpaths built-in-namespaces]
  (let [loader              (build-loader classpaths)
        built-in-namespaces (set built-in-namespaces)]
    (fn [{:keys [namespace]}]
      (when-not (contains? built-in-namespaces namespace)
        (source-for-namespace loader namespace)))))
