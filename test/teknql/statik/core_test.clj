(ns teknql.statik.core-test
  (:require [teknql.statik.core :as sut :refer [def-asset]]
            [clojure.test :as t :refer [deftest testing is]]))


(def html-asset
  {:type :html
   :path "index.html"
   :data [:title "Something"]})

(def css-asset
  {:type :css
   :path "css/global.css"
   :data [:body {:background-color :red}]})


(deftest compile-test
  (testing "html asset"
    (is (= {"index.html" "<title>Something</title>"}
           (sut/compile [html-asset]))))

  (testing "css asset"
    (is (= {"css/global.css" "body {\n  background-color: red;\n}"}
           (sut/compile [css-asset])))))
