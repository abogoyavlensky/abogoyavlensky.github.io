End-to-end (e2e) tests are a great way to ensure your application works as expected, simulating production-like conditions as closely as possible. 
This way, you can test how your application behaves from a real user's perspective.
In this article, we'll explore how to run end-to-end tests in Clojure using an awesome library called Etaoin.
To run such tests for a web application, you typically need a webdriver to control the browser. The most common approach is running it as a separate CLI command.
While this works fine, it requires having a webdriver installed on your machine. You'll need to manage versions and deal with installation in CI environments.
Another option is running it in a Docker container, but you still need to manage Docker images and docker-compose files.
This is where Testcontainers comes in handy - it lets you manage Docker containers right from your code without any extra configuration or mental overhead.

### Simple example

After trying several options I ended up with [docker-selenium](https://github.com/SeleniumHQ/docker-selenium) as a webdriver in Docker. It's up to date, and it has an option to run driver for different browsers on ARM, that is helpful for using on Apple Silicon processors.

For illustration purpose let's run a basic working example in REPL with an app running outside of our local machine.
We will test [https://clojure.org/](https://clojure.org/) has an `h2` title that contains text "The Clojure Programming Language". 

First of all, we will need following dependencies in our deps.edn file:
```clojure
{
  ...
  :aliases 
  {:test {:extra-deps {etaoin/etaoin {:mvn/version "1.1.42"}
                       clj-test-containers/clj-test-containers {:mvn/version "0.7.4"}
                       org.testcontainers/testcontainers {:mvn/version "1.20.4"}}}}
  ...
}
```

Then we can start REPL, require dependencies and run our test:

```clojure
(ns demo
  (:require [clj-test-containers.core :as tc]
            [etaoin.api :as etaoin]))

(let [webdriver-port 4444
      container (-> (tc/create {:image-name "selenium/standalone-chromium:131.0"
                                :exposed-ports [webdriver-port]})
                    (tc/start!))
      driver (etaoin/chrome-headless {:port (get (:mapped-ports container) webdriver-port)
                                      :host (:host container)
                                      :args ["--no-sandbox"]})]

  (etaoin/go driver "https://clojure.org")
  (etaoin/visible? driver {:tag :h2
                           :fn/has-text "The Clojure Programming Language"}))  
```

Last expression should return `true`. In this snippet, first, we run a container using library `clj-test-containers` that is a convenient wrapper for an official Testcontainers library. Then we create a driver which we eventually use for getting a web page and checking the title content.

### Test local server

Usually we run tests against local server that we are developing. Having webdriver running in Docker container we have to be able to expose local port from host machine into Docker container. For this purpose there is a function `Testcontainers/exposeHostPorts`. We should run it after have started the server and before we run the test. To be able to request this server inside testcontainer we need to call `http://host.testcontainers.internal` with appropriate port instead of localhost.

For an example, we will start a Jetty server on port 8080 and check if `h2` title contains text "The Clojure Programming Language". To run server we need a couple of additional depenedencies:

```
compojure/compojure {:mvn/version "1.7.1"}
ring/ring-jetty-adapter {:mvn/version "1.13.0"}
```

Let's change previous example with addition of our own local server:

```diff
  (ns demo
    (:require [clj-test-containers.core :as tc]
              [etaoin.api :as etaoin]
              [compojure.core :as compojure]
              [ring.adapter.jetty :as jetty])
+   (:import [org.testcontainers Testcontainers]))

+ (compojure/defroutes app
+   (compojure/GET "/" [] "<h2>The Clojure Programming Language</h2>"))

+ (jetty/run-jetty app {:port 8000 :join? false})

; Expose local port into Docker container 
+ (Testcontainers/exposeHostPorts (int-array [8000]))

  (let [webdriver-port 4444
        container (-> (tc/create {:image-name "selenium/standalone-chromium:131.0"
                                  :exposed-ports [webdriver-port]})
                      (tc/start!))
        driver (etaoin/chrome-headless {:port (get (:mapped-ports container) webdriver-port)
                                        :host (:host container)
                                        :args ["--no-sandbox"]})]

-   (etaoin/go driver "https://clojure.org")  
+   (etaoin/go driver "http://host.testcontainers.internal:8000")
    (etaoin/visible? driver {:tag :h2
                             :fn/has-text "The Clojure Programming Language"}))
```

If run this snippet we should see `true` as a result of last expression. We run server on port 8000 and exposed it into Docker container. So we are able to test a web page from localhost inside testcontainer.


