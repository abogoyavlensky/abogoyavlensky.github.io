End-to-end (e2e) tests are a great way to ensure your application works as expected by simulating production-like conditions as closely as possible. They let you test how your application behaves from a real user's perspective.

In this article, we'll explore how to run end-to-end tests in Clojure using an awesome library called [Etaoin](https://github.com/clj-commons/etaoin). To run such tests for a web application, you typically need a webdriver to control the browser. The most common approach is running it as a separate CLI tool.

While this approach works, it requires having a webdriver installed on your machine. You'll need to manage versions and handle installation in CI environments. Another option is running it in a Docker container, but this still requires managing Docker images and docker-compose files.

This is where Testcontainers shines - it allows you to manage Docker containers directly from your code without any additional configuration or mental overhead.

### Simple example

After evaluating several options, I settled on [docker-selenium](https://github.com/SeleniumHQ/docker-selenium) as a webdriver in Docker. It's actively maintained and provides excellent support for running drivers on various architectures, including ARM - particularly beneficial when working with Apple Silicon processors.

Let's start with a basic working example in REPL using an external website. We'll test if [https://clojure.org/](https://clojure.org/) has an `h2` title containing the text "The Clojure Programming Language".

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

The last expression should return `true`. In this example, we first create a container using the `clj-test-containers` library (a convenient wrapper for the Testcontainers library). Then we initialize a driver that we'll use to load the web page and verify the title content.

### Test local server

In real-world scenarios, we often need to test against a local development server. When the webdriver runs in a Docker container, we need to expose the local port from the host machine to the Docker container. For this, we use the `exposeHostPorts` function from Testcontainers. This should be called after starting the server but before running the test. To access this server from within the testcontainer, instead of `localhost` we use `http://host.testcontainers.internal` with the appropriate port.

Let's create a Jetty server on port `8000` and verify if the `h2` title contains the text "The Clojure Programming Language". We'll need these additional dependencies:

```
compojure/compojure {:mvn/version "1.7.1"}
ring/ring-jetty-adapter {:mvn/version "1.13.0"}
```

Let's modify our previous example by adding our own local server:

```diff
  (ns demo
    (:require [clj-test-containers.core :as tc]
              [etaoin.api :as etaoin]
+             [compojure.core :as compojure]
+             [ring.adapter.jetty :as jetty])
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

Running this snippet should return `true`. We've successfully set up a server on port `8000` and exposed it to the Docker container, enabling us to test our local web page inside the testcontainer.

That covers the basics! You can now run tests against your local server using webdriver with Testcontainers. As a bonus, let's explore how to integrate this approach with an application system.

### Integrate with application system

In a real-world application, you'll likely use a component system to manage your application's lifecycle. Let's see how to integrate this approach with [Integrant](https://github.com/weavejester/integrant).

First, we'll need one more dependency:

```clojure
integrant/integrant {:mvn/version "0.13.1"}
```

Let's configure our system:

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

And a component for webdriver that we can enable just in test system:

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

Now in your `deftest`, you can access the `driver` from the `webdriver` component of the test system to run your assertions using Etaoin.

By setting `.withReuse` to `true` and enabling the `TESTCONTAINERS_REUSE_ENABLE=true` environment variable, you can reuse containers between tests.
This is why we don't stop the container in the `halt-key!` method. Container reuse significantly reduces test execution time during local development, as you do not wait while container starts.
In a CI environment, we omit the `TESTCONTAINERS_REUSE_ENABLE` variable to disable reuse. The JVM process automatically stops all containers when it terminates, so explicit cleanup in `halt-key!` isn't necessary.

### Wrapping up

In this article, we've explored how to set up end-to-end tests in Clojure using Etaoin and Testcontainers. We started with a simple example, progressed to testing a local server, and finally integrated the solution with a component system using Integrant. This approach eliminates the hassle of managing separate webdrivers and Docker configurations, making e2e testing more straightforward and maintainable.
