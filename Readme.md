# Statik

CLI static site generator using an embedded clojre DSL. Uses hiccup and garden
under the hood.


## Example

`site.clj`
```clj
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
   :data (page "Hello world" [:p "This is magic"])})
```

```
statik compile site.clj

# Generates:

out/
  index.html
  css/global.css
```

## Namespaces and Requires

Statik includes the following namespaces by default:

- `garden.core`
- `garden.stylesheet`
- `garden.color`
- `garden.units`

You can also require your own namespaces, assuming you are running from a root directory, `src/`
will be on the classpath.

## Usage

### Live Reload
```
statik watch src/site.clj
# Starts a (live-reloading) server on localhost:3000
```

### Serving
```
# Start a staitc server in the current directory
statik serve

# Start a staitc server in out/
statik serve out/
```

## Building

```
clj -Anative-image
```
