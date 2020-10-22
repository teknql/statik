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

## Building and Installing

To build statik you will need Graal v20.2.0 w/ OpenJDK v11, as well as the native-image plugin.


### Arch / Manjaro

```sh
# Install via Yay / Pacman
yay -S native-image-jdk11-bin jdk11-graalvm-bin

# Set as the default JVM
sudo archlinux-java set java-11-graalvm

# Ensure its in your PATH
export PATH=/usr/lib/jvm/default/bin:$PATH
```

### Installing Graal From Scratch

Download the appropriate release of Graal for you VM from [the releases page](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-20.2.0).

```sh
# Untar it
tar -xvzf graalvm-ce-java11-linux-amd64-20.2.0.tar.gz

# Install it
sudo mv ./graalvm-ce-java11-20.2.0 /usr/lib/jvm/java-11-graalvm

# Set it as the default JVM
sudo ln -s /usr/lib/jvm/java-11-graalvm /usr/lib/jvm/default
sudo ln -s /usr/lib/jvm/java-11-graalvm /usr/lib/jvm/default-runtime

# Ensure its in your PATH
export PATH=/usr/lib/jvm/default/bin:$PATH

# Install Native Image
gu install native-image
```

### Compiling Statik

```sh
# Compile
cd /path/to/statik
mkdir build ;; This is the output dir, but its not version controlled
clj -M:native-image

# Install Binary
cp build/statik ~/.local/bin

# Test and Enjoy
statik --help
```
