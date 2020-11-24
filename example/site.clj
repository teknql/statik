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

(def-asset about-page
  {:path "/about.html"
   :type :html
   :data (let [{title :title
                content :content}
               (parse-org "./example/blog/about.org")]
           (page title content))})

(def-asset home-page
  {:path "/index.html"
   :type :html
   :data (page "Hello world" [:a {:href (asset-path about-page)} "This is a blog link."])})
