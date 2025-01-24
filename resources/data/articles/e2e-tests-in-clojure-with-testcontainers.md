End-to-end (e2e) tests are a great way to ensure your application works as expected by simulating production-like conditions as closely as possible. They let you test how your application behaves from a real user's perspective.

In this article, we'll explore how to run end-to-end tests in Clojure using an awesome library called [Etaoin](https://github.com/clj-commons/etaoin). To run such tests for a web application, you typically need a webdriver to control the browser. The most common approach is running it as a separate CLI command.

While this works fine, it means you need to have a webdriver installed on your machine. You'll need to manage versions and deal with installation in CI environments. Another option is running it in a Docker container, but you still need to manage Docker images and docker-compose files.

This is where Testcontainers comes in handy - it lets you manage Docker containers right from your code without any extra configuration or mental overhead.

### Simple example

After trying several options, I settled on [docker-selenium](https://github.com/SeleniumHQ/docker-selenium) as a webdriver in Docker. It's up to date and supports running drivers for different browsers on ARM, which is particularly useful when working with Apple Silicon processors.

For illustration purposes, let's run a basic working example in REPL with an app running outside of our local machine. We'll test if [https://clojure.org/](https://clojure.org/) has an `h2` title containing the text "The Clojure Programming Language".

First, we'll need the following dependencies in our deps.edn file:

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

Then we can start REPL, require dependencies, and run our test:

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

The last expression should return `true`. In this snippet, we first run a container using the `clj-test-containers` library (a convenient wrapper for the official Testcontainers library). Then we create a driver that we'll use to load the web page and check the title content.

### Test local server

Typically, we run tests against a local server that we're developing. When the webdriver is running in a Docker container, we need to expose the local port from the host machine to the Docker container. For this, we use the `Testcontainers/exposeHostPorts` function. We should run it after starting the server but before running the test. To request this server inside the testcontainer, we need to use `http://host.testcontainers.internal` with the appropriate port instead of localhost.

Let's start a Jetty server on port 8080 and check if the `h2` title contains the text "The Clojure Programming Language". To run the server, we need a couple more dependencies:

```
compojure/compojure {:mvn/version "1.7.1"}
ring/ring-jetty-adapter {:mvn/version "1.13.0"}
```

Let's change our previous example by adding our own local server:

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

If we run this snippet, we should see `true` as the result. We're running a server on port 8000 and exposing it to the Docker container, which lets us test our local web page inside the testcontainer.

That's basically it! Now we can run tests against our local server using webdriver with Testcontainers. But as a bonus, let's look at how to integrate this approach with an application system.

### Integrate with application system

In a real-world application, we'll likely be using some kind of component system to manage our application's lifecycle. Let's see how we can integrate this approach with [Integrant](https://github.com/weavejester/integrant).

Let's modify the previous example to use Integrant. We need one more dependency:

```clojure
integrant/integrant {:mvn/version "0.13.1"}
```

First, we need to configure our system:

*resources/config.end*
```clojure
{:app.server/server {}
 :app.server/webdriver {:server #ig/ref :app.server/server}}
```

Then create a component for the server:

*src/app/server.clj*
```clojure
(ns app.server
  (:require [compojure.core :as compojure]
            [ring.adapter.jetty :as jetty]
            [integrant.core :as ig])

(compojure/defroutes app
  (compojure/GET "/" [] "<h2>The Clojure Programming Language</h2>"))

(defmethod ig/init-key ::server
  [_ _]
  (jetty/run-jetty app {:port 8000 :join? false}))

(defmethod ig/halt-key! ::server
  [_ server]
  (.stop server))
```

And a component for webdriver that we can use just in test system:

*test/app/webdriver.clj*
```clojure
(ns app.webdriver
  (:require [clj-test-containers.core :as tc]
            [etaoin.api :as etaoin]
            [compojure.core :as compojure]
            [ring.adapter.jetty :as jetty]
            [integrant.core :as ig])
  (:import [org.testcontainers Testcontainers]))

(defmethod ig/init-key ::webdriver
  [_ {:keys [server]}]
  (let [webdriver-port 4444
        server-port (.getLocalPort (first (.getConnectors server)))
        ; Expose port from local machine to container
        _ (Testcontainers/exposeHostPorts (int-array [server-port]))
        container (-> (tc/create {:image-name "selenium/standalone-chromium:131.0"
                                  :exposed-ports [webdriver-port]})
                      (update :container #(.withReuse % true))
                      (tc/start!))]

    {:container container
     :driver (etaoin/chrome-headless {:port (get (:mapped-ports container) webdriver-port)
                                      :host (:host container)
                                      :args ["--no-sandbox"]})}))

(defmethod ig/halt-key! ::webdriver
  [_ {:keys [driver]}]
  (log/info (str "[DB] Closing webdriver..."))
  ; Do not stop the container to be able to reuse it
  (etaoin/quit driver))
```

Then, in `deftest` we will be able to get `driver` from the `webdriver` component of the test system and run our assertions using Etaoin.
Setting up the `.withReuse` to `true` in combination with `TESTCONTAINERS_REUSE_ENABLE=true` env variable will allow us to reuse the container between tests.
That's why we do not stop the container in the `halt-key!` method. Being able to reuse the container is useful, because it saves a lot of time when running tests locally. 
In CI environment we do not set `TESTCONTAINERS_REUSE_ENABLE` var to disable the reuse. In this case in CI all containers will be stopped automatically along with the JVM process, so we don't need to do it in `halt-key!` explicitly.


### Wrapping up

In this article, we've explored how to set up end-to-end tests in Clojure using Etaoin and Testcontainers. We started with a simple example, progressed to testing a local server, and finally integrated the solution with a component system using Integrant. This approach eliminates the hassle of managing separate webdrivers and Docker configurations, making e2e testing more straightforward and maintainable.
