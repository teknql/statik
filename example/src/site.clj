(ns site
  (:require [require-me]))

(def site-name "My amazing website")

(def-asset global-style
  {:path "/css/global.css"
   :type :css
   :data [:body {:background-color :red}]})

(defn page
  [title content]
  [:html
   [:head
    [:title (str site-name " - " title)]
    (stylesheet global-style)]
   [:body content]])

(def-asset home-page
  {:path "/index.html"
   :type :html
   :data (page "Hello world" (require-me/foo) #_[:p "This is magic"])})
