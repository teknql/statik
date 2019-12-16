(ns teknql.statik.watch-middleware
  "Watch middleware.
  A rip off of https://github.com/weavejester/ring-refresh which fixes the run-time reflection for
  GraalVM."
  (:use [compojure.core :only (routes GET)]
        ring.middleware.params)
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [teknql.statik.watcher :as watcher])
  (:import [java.util Date UUID]))

(set! *warn-on-reflection* true)

(defn- get-request? [request]
  (= (:request-method request) :get))

(defn- success? [response]
  (<= 200 (:status response) 299))

(defn- html-content? [response]
  (if-let [content-type (get-in response [:headers "Content-Type"])]
    (re-find #"text/html" content-type)))

(def ^:private refresh-script
  (slurp (io/resource "ring/js/refresh.js")))

(defprotocol AsString
  (as-str [x]))

(extend-protocol AsString
  String
  (as-str [s] s)
  java.io.File
  (as-str [f] (slurp f))
  java.io.InputStream
  (as-str [i] (slurp i))
  clojure.lang.ISeq
  (as-str [xs] (apply str xs))
  nil
  (as-str [_] nil))

(defn- add-script [body script]
  (if-let [body-str (as-str body)]
    (str/replace
     body-str
     #"<head\s*[^>]*>"
     #(str % "<script type=\"text/javascript\">" script "</script>"))))

(def ^:private last-modified
  (atom (Date.)))

(defn- watch-dirs! [dirs]
  (watcher/watch dirs #(reset! last-modified (Date.))))

(defn- random-uuid []
  (str (UUID/randomUUID)))

(defn- watch-until [reference pred timeout-ms]
  (let [result    (promise)
        watch-key (random-uuid)]
    (try
      (add-watch reference
                 watch-key
                 (fn [_ _ _ value]
                   (when (pred value)
                     (deliver result true))))
      (or (pred @reference)
          (deref result timeout-ms false))
      (finally
        (remove-watch reference watch-key)))))

(def ^:private source-changed-route
  (GET "/__source_changed" [^String since]
    (let [timestamp (Long. since)]
      (str (watch-until
            last-modified
            #(> (.getTime ^Date %) timestamp)
            60000)))))

(defn- wrap-with-script [handler script]
  (fn [request]
    (let [response (handler request)]
      (if (and (get-request? request)
               (success? response)
               (html-content? response))
        (-> response
            (update-in [:body] add-script script)
            (update-in [:headers] dissoc "Content-Length"))
        response))))

(defn wrap-refresh
  "Injects Javascript into HTML responses which automatically refreshes the
  browser when any file in the supplied directories is modified. Only successful
  responses from GET requests are affected. The default directories are 'src'
  and 'resources'."
  ([handler]
   (wrap-refresh handler ["src" "resources"]))
  ([handler dirs]
   (watch-dirs! dirs)
   (wrap-params
    (routes
     source-changed-route
     (wrap-with-script handler refresh-script)))))
