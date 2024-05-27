### TL;DR

To get a quick example of a Clojure app setup with full deployment configuration including API service, frontend with ClojureScript
and a PostgreSQL, check out the [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example) 
project repository. To try deployment yourself, simply clone the repo and locally execute 
the commands from the [Deploy: summary](https://github.com/abogoyavlensky/clojure-kamal-example/tree/master?tab=readme-ov-file#deploy-summary)
section. If you already have a Docker image that exposes port 80
you can skip project setup part and go straight to the "Deployment config" section of this article.

### Overview

Sometimes I need to quickly deploy an app without worrying 
too much about scalability. In this case, it's fine to deploy an app on
a single server. This approach works for small/medium non-critical apps,
staging, or preview environments. In this article, 
I'm going to show how we can configure [Kamal](https://kamal-deploy.org/) to deploy 
a full-stack Clojure/Script app on a single server with a Let's Encrypt TLS certificate and the 
ability to run multiple apps. The good part is that when the time comes, 
you can add a load balancer in front of the app and scale 
to multiple servers.

I'm going to start by highlighting the important parts of 
the application setup. Then we will overview 
the deployment config, perform server setup, and conduct the first deployment.
We will also briefly check how we can manage our app on the server. 
Finally, we will configure a full CI/CD process to automatically deploy 
our app to the server from GitHub Actions.  

### Project setup

We are going to set up a web app with Clojure API server on backend, ClojureScript with Re-frame on frontend
and PostgreSQL as a main database. You can check the example project in the repository [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example).
It has backend and frontend parts, the important parts of the project's structure
looks like this:

```text
clojure-kamal-example
├── README.md
├── .tool-versions
├── Dockerfile
├── Taskfile.yaml
├── deps.edn
├── build.clj
├── package.json
├── shadow-cljs.edn
├── tailwind.config.js
├── .github
│   ├── actions/
│   └── workflows
│       ├── checks.yaml
│       └── deploy.yaml
├── config
│   └── deploy.yml
├── dev
│   └── user.clj
├── resources
│   └── db
│   │   ├── migrations/
│   │   └── models.edn
│   ├── public
│   │   ├── css
│   │   │   └── input.css
│   │   └── index.html
│   ├── config.edn
│   └── logback.xml
├── src
│   ├── clj
│   │   └── api
│   │       ├── util/
│   │       ├── db.clj 
│   │       ├── handler.clj 
│   │       ├── server.clj 
│   │       └── main.clj 
│   ├── cljc
│   │   └── common
│   │       └── api_routes.cljc
│   └── cljs
│       └── ui
│           ├── util/
│           ├── db.cljs
│           ├── events.cljs
│           ├── router.cljs
│           ├── subs.cljs
│           ├── views.cljs
│           └── main.cljs
└── test/
```

Here we use general names `api`, `ui`, `common` for namespace prefix of each application part.
I quite like this approach because it unifies those parts and it's a bit easier 
to switch between different projects. 

Speaking about libraries and tools, we are using: [Integrant](https://github.com/weavejester/integrant) for app system management, 
[Reitit](https://github.com/metosin/reitit) for routing on the backend and frontend, 
[Malli](https://github.com/metosin/malli) for data validation, [Automigrate](https://github.com/abogoyavlensky/automigrate) for managing database migrations. 
On the frontend we are using ClojureScript with [Re-frame](https://github.com/day8/re-frame), [Shadow CLJS](https://github.com/thheller/shadow-cljs) as a build system, and [Tailwind CSS](https://tailwindcss.com/) for styling.
For managing app locally and in CI we use [Taskfile](https://taskfile.dev/) as a replacement for Make and [mise-en-place](https://mise.jdx.dev/) for system tools version management.

Deps.edn for the project looks like:

_deps.edn_
```clojure
{:deps {org.clojure/clojure {:mvn/version "1.11.3"}
        ; Logging
        org.clojure/tools.logging {:mvn/version "1.3.0"}
        ch.qos.logback/logback-classic {:mvn/version "1.5.6"}
        ; System & Config
        integrant/integrant {:mvn/version "0.10.0"}
        aero/aero {:mvn/version "1.1.6"}
        metosin/malli {:mvn/version "0.16.1"}
        ; Server
        metosin/reitit {:mvn/version "0.7.0"}
        ring/ring-jetty-adapter {:mvn/version "1.12.1"}
        amalloy/ring-gzip-middleware {:mvn/version "0.1.4"}
        ; db
        org.postgresql/postgresql {:mvn/version "42.7.3"}
        hikari-cp/hikari-cp {:mvn/version "3.1.0"}
        com.github.seancorfield/next.jdbc {:mvn/version "1.3.939"}
        com.github.seancorfield/honeysql {:mvn/version "2.6.1126"}
        net.clojars.abogoyavlensky/automigrate {:mvn/version "0.3.3"}}

 :paths ["src/clj" "src/cljc" "resources"]

 :aliases {:dev {:extra-paths ["dev"]
                 :extra-deps {ring/ring-devel {:mvn/version "1.12.1"}
                              integrant/repl {:mvn/version "0.3.3"}}}

           :cljs {:extra-paths ["src/cljs"]
                  :extra-deps {org.clojure/clojurescript {:mvn/version "1.11.132"}
                               metosin/reitit-frontend {:mvn/version "0.7.0"}
                               re-frame/re-frame {:mvn/version "1.4.3"}
                               reagent/reagent {:mvn/version "1.2.0"
                                                :exclusions [cljsjs.react-dom/cljsjs.react-dom]}
                               day8.re-frame/http-fx {:mvn/version "0.2.4"}
                               cljs-ajax/cljs-ajax {:mvn/version "0.8.4"}}}

           :shadow {:extra-deps {thheller/shadow-cljs {:mvn/version "2.28.8"}}
                    :main-opts ["-m" "shadow.cljs.devtools.cli"]}

           :test {:extra-paths ["test/clj" "test/cljs"]
                  :extra-deps {eftest/eftest {:mvn/version "0.6.0"}
                               cloverage/cloverage {:mvn/version "1.2.4"}
                               hato/hato {:mvn/version "0.9.0"}
                               clj-test-containers/clj-test-containers {:mvn/version "0.7.4"}
                               org.testcontainers/postgresql {:mvn/version "1.19.8"}}
                  :exec-fn cloverage.coverage/run-project
                  :exec-args {:test-ns-path ["test"]
                              :src-ns-path ["src"]
                              :runner :eftest
                              :runner-opts {:multithread? false}}}

           :outdated {:extra-deps {com.github.liquidz/antq {:mvn/version "2.8.1201"}}
                      :main-opts ["-m" "antq.core" "--no-diff"]}

           :migrations {:ns-default automigrate.core}

           :build {:deps {io.github.clojure/tools.build {:git/tag "v0.10.3" :git/sha "15ead66"}}
                   :ns-default build}}}
```

For demonstration purposes I added a couple of database [models](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/resources/db/models.edn) `movie` and `director`, an API route 
to get all records from the `movie` model and the representation of that list on the web page.
API routes are defined in the [common cljc-directory](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/src/cljc/common/api_routes.cljc) to get the opportunity to use 
API routes on frontend by names from single source of truth.


#### Backend

The app system looks like:

```clojure
{:api.db/db {:options 
             {:jdbc-url #profile {:default #env DATABASE_URL
                                  :test "jdbc:tc:postgresql:15.2-alpine3.17:///testdb?TC_DAEMON=true"}}}

 :api.handler/handler {:options 
                       {:reloaded? #profile {:default false
                                             :dev true}
                        :cache-assets? #profile {:default false
                                                 :prod true}}
                       :db #ig/ref :api.db/db}

 :api.server/server {:options 
                     {:port #profile {:default 8000
                                      :prod 80
                                      :test #free-port true}}
                     :handler #ig/ref :api.handler/handler}}
```

System contains three components: 
- `:api.db/db` - database connection pool;
- `:api.handler/handler` - application handler with API Reitit-router based on ring and middlewares;
- `:api.server/server` - Jetty server. 

I like the approach to group component config options into `:options` key 
to do not mix them with references to other components.

Here we use [Aero](https://github.com/juxt/aero) to extend system config with useful data readers.
There is a `#profile` reader to switch between `dev`, `test` and `prod`; 
`#env` for reading environment variables. It is extended with Integrant's `#ig/ref` to use components 
as references in other components. Also, I added `#free-port` to pick free port for
API web server while it's starting in tests:

_src/clj/api/util/system.clj_
```clojure
(ns api.util.system
  (:require ...
            [aero.core :as aero])
  (:import (java.net ServerSocket)))

...
(defmethod aero/reader 'free-port
  [_ _ _value]
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))
...
```

In [handler](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/src/clj/api/handler.clj) 
there are options to enable auto-reloading backend code on any change without restarting the whole system
by using `:reloaded?` and `:cache-assets?` to enable static assets cashing in production. 
You can read about the approach for auto-reloading in the another [article](https://bogoyavlensky.com/blog/auto-reloading-ring/). 

During tests we automatically start database as a Docker container using Testcontainers's
feature [JDBC support](https://java.testcontainers.org/modules/databases/jdbc/#using-postgresql).
All we need is to add `tc:` prefix after `jdbc:` in the JDBC URL 
and management database container will be done by Testcontainers under the hood.
To speed up tests we use `TC_DAEMON=true` parameter to JDBC URL to reuse the same
container for multiple tests.

#### Frontend

On frontend we use [`reitit.frontend.easy/start!`](https://github.com/abogoyavlensky/clojure-kamal-example/blob/7f9e07a3bfc44aaa60323a22d6c13ded2a232dd6/src/cljs/ui/router.cljs#L35) to configure a router on the frontend.
To render the main page we use `re-frame.core/create-root` to be able to use latest React
versinos (=> 18.x)

_ui/main.cljs_
```clojure
(ns ui.main
  (:require [reagent.dom.client :as reagent]
            [re-frame.core :as re-frame]
            [ui.views :as views]
            [ui.router :as router]
            [ui.events :as events]
            [ui.subs]))

(defonce ^:private ROOT
         (reagent/create-root (.getElementById js/document "app")))

(defn render!
  "Render the page with initializing routes."
  []
  (re-frame/clear-subscription-cache!)
  (router/init-routes!)
  (reagent/render
    ROOT
    [views/router-component {:router router/router}]))

(defn init!
  "Render the whole app with default db value."
  []
  (re-frame/dispatch-sync [::events/initialize-db])
  (render!))
```

To build css for development and in production we use Tailwind CSS 
js library directly via `npx`.

Not quite much to add, the frontend setup is pretty basic for re-frame app.

### Build docker image

I tried to make Dockerfile as simple and transparent as possible,
so I don't use Taskfile and mise here. I use Alpine as a base image. 
There are two stages image: 
- build stage - build of uberjar with all minified and hashed frontend static files;
- result stage - final image with only the uberjar from the previous stage.  

```dockerfile
FROM --platform=linux/amd64 clojure:temurin-21-tools-deps-1.11.3.1456-alpine AS build

WORKDIR /app

# Install npm
RUN echo "http://dl-cdn.alpinelinux.org/alpine/v3.20/community" >> /etc/apk/repositories
RUN apk add --update --no-cache npm=10.8.0-r0

# Node deps
COPY package.json package-lock.json /app/
RUN npm i

# Clojure deps
COPY deps.edn  /app/
RUN clojure -P -X:cljs:shadow

# Build ui and uberjar
COPY . /app
RUN npx tailwindcss -i ./resources/public/css/input.css -o ./resources/public/css/output-prod.css --minify \
    && clojure -M:dev:cljs:shadow release app \
    && clojure -T:build build


FROM --platform=linux/amd64 eclipse-temurin:21.0.2_13-jre-alpine
LABEL org.opencontainers.image.source=https://github.com/abogoyavlensky/clojure-kamal-example

WORKDIR /app
COPY --from=build /app/target/standalone.jar /app/standalone.jar
RUN apk add --no-cache curl

EXPOSE 80
CMD ["java", "-Xmx256m", "-jar", "standalone.jar"]
```

We are going to deploy on amd64 architecture, so to be able 
to deploy first time from macOS we added `--platform=linux/amd64`
to the `FORM` definition.

At build step we run css, js and uberjar builds separately one by one. 
We are going to publish images to GitHub ghcr.io registry, so it's convenient to 
link uploaded images with repository by default for this purpose we added `LABEL`
to the final image definition. We added `-Xmx256m` option to java command, 
as it allows us to deploy to a small instance, you can extend or remove this config
as you prefer.

### Deployment config

Kamal is just a thin wrapper around Docker. So nearly everything can be customized
and re-configured. It has predefined scripts to bootstrap servers with
installation of cURL and Docker. It also has default config for Traefik as it uses as default 
reverse proxy server to route all traffic to the app. And it has handy cli tool to manage service
on the host: build, deploy, read logs, exec commands, etc.

Let's look at full deployment config for Kamal that includes traefik, web app and database configuration.

_config/deploy.yaml_
```yaml
service: clojure-kamal-example
image: <%= ENV['REGISTRY_USERNAME'] %>/clojure-kamal-example

servers:
  web:
    hosts:
      - <%= ENV['SERVER_IP'] %>
    labels:
      traefik.http.routers.clojure-kamal-example.rule: Host(`<%= ENV['APP_DOMAIN'] %>`)
      traefik.http.routers.clojure-kamal-example.tls: true
      traefik.http.routers.clojure-kamal-example.entrypoints: websecure
      traefik.http.routers.clojure-kamal-example.tls.certresolver: letsencrypt
    options:
      network: "traefik"

registry:
  server: ghcr.io
  username:
    - REGISTRY_USERNAME
  password:
    - REGISTRY_PASSWORD

builder:
  multiarch: false
  cache:
    type: gha
    options: mode=max

healthcheck:
  path: /health
  port: 80
  exposed_port: 4001
  max_attempts: 15
  interval: 30s

env:
  secret:
    - DATABASE_URL

# Database
accessories:
 db:
   image: postgres:15.2-alpine3.17
   host: <%= ENV['SERVER_IP'] %>
   env:
     secret:
       - POSTGRES_DB
       - POSTGRES_USER
       - POSTGRES_PASSWORD
   directories:
     - clojure_kamal_example_postgres_data:/var/lib/postgresql/data
   options:
     network: "traefik"

# Traefik
traefik:
  options:
    publish:
      - "443:443"
    network: "traefik"
    volume:
      - "/root/letsencrypt:/letsencrypt"
  args:
    entrypoints.web.address: ":80"
    entrypoints.websecure.address: ":443"
    # TLS-certificate configuration
    certificatesResolvers.letsencrypt.acme.email: <%= ENV['TRAEFIK_ACME_EMAIL'] %>
    certificatesResolvers.letsencrypt.acme.storage: "/letsencrypt/acme.json"
    certificatesResolvers.letsencrypt.acme.tlschallenge: true
    certificatesResolvers.letsencrypt.acme.httpchallenge.entrypoint: web
    # Redirect to HTTPS by default
    entryPoints.web.http.redirections.entryPoint.to: websecure
    entryPoints.web.http.redirections.entryPoint.scheme: https
    entryPoints.web.http.redirections.entrypoint.permanent: true
```

We configured Traefik with aditional arguments with prefix `certificatesResolvers` and volume
to automatically add TLS certificated using Let's Encrypt. 
Also, we added a couple of `entryPoints` arguments to automatically redirect from `http` to `https`.

We added web service configuration with traefik labels to configure domain for the app:
```yaml
servers:
  web:
    hosts:
      - <%= ENV['SERVER_IP'] %>
    labels:
      ...
    options:
      network: "traefik"
```
We are going to read server IP from the env var, so we use Ruby template syntax
for it `<%= ENV['SERVER_IP'] %>`. In case you want to deploy to multiple servers
you can read multiple IPs from the single env var contained string with IPs separate
with comma, and then read it in config like this: `hosts: <%= ENV['SERVER_IPS'].split(',') %>`.

Our app contains just an initial setup with database connection so at the moment 
we need to configure just jdbc url in env vars:
```yaml
env:
  secret:
    - DATABASE_URL
```

For all services we use the same docker network in our case it is called `traefik`.
The name for network can be anyone you want. Custom Docker network is needed to 
get access from the app to the database ran on **the same host**. So, if, for instance,
you run database on a different host or use third-party service like Supabase or Neon, 
you don't need setup Docker network.

We run database as accessory on the same host with configuration for secrets 
and directories to store data.

We are going to use GitHub registry as a Docker registry for pushing docker images 
with our app. But is possible to use any registry you want just change `registry.server` value.

We will use Kamal to build the Docker image of the app, and we are going to 
use GitHub Actions as our CI/CD service so there is configuration for caching 
to speed up builds. We are going to deploy on amd64 architecture, so we don't 
to waste time on building multiple images for each platform, so the simplest solution
is disabling multiarch build.

```yaml
builder:
  multiarch: false
  cache:
    type: gha
    options: mode=max
```

Finally, we adjusted healthcheck config with custom path, port 
and attempts settings:   

```yaml
healthcheck:
  path: /health
  port: 80
  exposed_port: 4001
  max_attempts: 15
  interval: 30s
```

### Initial deployment

First of all we need to bootstrap server with initial installation and 
initial deployment of the app and other services. 

#### Pre-requisites

- Docker installed on local machine.
- Domain.
- Server with public IP.
- SSH connection from local machine to the server with SSH-keys.
- Open 443 and 80 ports on server.
- (optional) Configure firewall with open just 443, 80 and 22 ports.

An example of firewall config for server might look like:
![Firewall config](/assets/images/articles/8_firewall_config.png)

#### Install Kamal locally

Install [mise-en-place](https://mise.jdx.dev/getting-started.html#quickstart) (or [asdf](https://asdf-vm.com/guide/getting-started.html)),
and run:

```shell
brew install libyaml  # or on Ubuntu: `sudo apt-get install libyaml-dev` 
mise install ruby
gem install kamal -v 1.5.2
kamal version
```


---
_**Note**: Alternatively you can use dockerized version of Kamal 
by running `./kamal.sh` predefined command instead of Ruby gem version. 
It mostly works for initial server setup, but some management commands don't work properly.
For instance, `./kamal.sh app logs -f` or `./kamal.sh build push`._

---


#### Env variables

Run command `envify` to create a `.env` with all required empty variables:

```shell
kamal envify --skip-push
```

_The `--skip-push` parameter prevents the `.env` file from being pushed to the server._

Now, you can fill all environment variables in the `.env` file with actual values for deployment on the server.
Here’s an example:

```shell
# Generated by kamal envify
# DEPLOY
SERVER_IP=192.168.0.1
REGISTRY_USERNAME=your-username
REGISTRY_PASSWORD=secret-registry-password
TRAEFIK_ACME_EMAIL=your_email@example.com
APP_DOMAIN=app.domain.com

# App
DATABASE_URL="jdbc:postgresql://clojure-kamal-example-db:5432/demo?user=demoadmin&password=secret-db-password"

# DB accessory
POSTGRES_DB=demo
POSTGRES_USER=demoadmin
POSTGRES_PASSWORD=secret-db-password
```

Notes:
- `SERVER_IP` - the IP of the server you want to deploy your app, you should be able to connect to it using ssh-keys.
- `REGISTRY_USERNAME` and `REGISTRY_PASSWORD` - credentials for docker registry, in our case we are using `ghcr.io`, but it can be any registry.
- `TRAEFIK_ACME_EMAIL` - email for register TLS-certificate with Let's Encrypt and Traefik.
- `APP_DOMAIN` - domain of your app, should be configured to point to `SERVER_IP`.
- `clojure-kamal-example-db` - this is the name of the database container from accessories section of `deploy/config.yml` file.
- We duplicated database credentials to set up database container and use `DATABASE_URL` in the app.

:warning: _Do not include file `.env` to git repository!_

#### Bootstrap server and deploy app

Install Docker on a server:

```shell
kamal server bootstrap
```

Create a Docker network for access to the database container from the app by container name
and a directory for Let’s Encrypt certificates:

```shell
ssh root@192.168.0.1 'docker network create traefik'
ssh root@192.168.0.1 'mkdir -p /root/letsencrypt && touch /root/letsencrypt/acme.json && chmod 600 /root/letsencrypt/acme.json'
```

Set up Traefik, the database, environment variables and run app on a server:

```shell
kamal setup
```

The app is deployed on the server, but it is not fully functional yet. You need to run database migrations:

```shell
kamal app exec 'java -jar standalone.jar migrations'
```

Now, the application is fully deployed on the server! You can check it on youe domain.

---

#### A note about database migrations in production

In general I don't like to run database migrations as an additional step in the app system,
because in this case we don't have a full control of the migration process. 
So, I prefer to run migrations as a separate step in CD pipeline before deploy itself._

To be able to run migrations within jar-file I added second command
to the main function of the app. Automigrate by default reads env var `DATABASE_URL` and uses
models and migrations from the dir `resoureces/db`. So by default we don't need to configure anything
other than just set up database url env variable. The main function of the app looks like:  

_api.main.clj_
```clojure
(ns api.main
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [automigrate.core :as automigrate]
            [api.util.system :as system-util]))


(defn- run-system
  [profile]
  (let [profile-name-kw profile
        config (system-util/config profile-name-kw)]
    (log/info "[SYSTEM] System is starting with profile:" profile-name-kw)
    (ig/load-namespaces config)
    (-> config
        (ig/init)
        (system-util/at-shutdown))
    (log/info "[SYSTEM] System has been started successfully.")))


(defn -main
  "Run application system in production env."
  [& args]
  (case (first args)
    "migrations" (automigrate/migrate)
    (run-system :prod)))
```

Running jar without any arguments we will have app system running on port 80:
`java -jar standalone.jar`. If we need to run migrations just an additional 
argument to the command: `java -jar standalone.jar migrations`.

---

#### Regular deploy

For subsequent deployments from the local machine, run:

```shell
kamal deploy
```

Or just push to the master branch, there is a GitHub Actions pipeline that does
the deployment automatically `.github/workflows/deploy.yaml`. 
We will check it in detail in following section.


### Manage app on the server

Let's see a couple of handy commands to manage and inspect our application on the server.

Getting list of running containers:

```shell
kamal details -q

Traefik Host: 192.168.0.1
CONTAINER ID   IMAGE           COMMAND                  CREATED       STATUS       PORTS                                                                      NAMES
045e76b559e3   traefik:v2.10   "/entrypoint.sh --pr…"   3 weeks ago   Up 3 weeks   0.0.0.0:80->80/tcp, :::80->80/tcp, 0.0.0.0:443->443/tcp, :::443->443/tcp   traefik

App Host: 192.168.0.1
CONTAINER ID   IMAGE                                                                                   COMMAND                  CREATED          STATUS                    PORTS     NAMES
e1007ae82d0b   ghcr.io/abogoyavlensky/clojure-kamal-example:f0dce409b7cde87a22597a56f3f23e8a24374215   "/__cacert_entrypoin…"   12 minutes ago   Up 12 minutes (healthy)   80/tcp    clojure-kamal-example-web-f0dce409b7cde87a22597a56f3f23e8a24374215

Accessory db Host: 192.168.0.1
CONTAINER ID   IMAGE                      COMMAND                  CREATED       STATUS       PORTS                                       NAMES
da9d0b805330   postgres:15.2-alpine3.17   "docker-entrypoint.s…"   3 weeks ago   Up 3 weeks   5432/tcp   clojure-kamal-example-db
```

Follow application logs:

```shell
kamal app logs -f
...
```

Start an interactive shell session in the currently running container:

```shell
kamal app exec -i --reuse sh
Get current version of running container...
  ...
Launching interactive command with version f0dce409b7cde87a22597a56f3f23e8a24374215 via SSH from existing container on 192.168.0.1...
/app # 
```

Print app version:

```shell
kamal app version

...
  INFO [9dcdfdb6] Finished in 1.311 seconds with exit status 0 (successful).
App Host: 192.168.0.1
f0dce409b7cde87a22597a56f3f23e8a24374215
```

Stop or start current version of application:

```shell
kamal app stop
kamal app start
```

If you want to change traefik config, run:

```shell
kamal traefik reboot
```

And few more useful commands you could find by running:


```shell
kamal help
```

### CI/CD

At this moment we have application running on the server and ability 
to deploy and manage it from local machine. The next step would be to deploy 
the app from CI pipeline.


#### CI pipeline: environment variables

For CI setup you need to add following environment variables as secrets for Actions.
In GitHub UI of the repository navigate to `Settings -> Secrets and variables -> Actions`.
Then add variables with the same values you added to local `.env` file:

```shell
APP_DOMAIN
DATABASE_URL
POSTGRES_DB
POSTGRES_PASSWORD
POSTGRES_USER
SERVER_IP
SSH_PRIVATE_KEY
TRAEFIK_ACME_EMAIL
```

- `SSH_PRIVATE_KEY` - a new SSH private key **without password** that you created and added public part of it to servers's `~/.ssh/authorized_keys` to authorize from CI-worker.

To generate SSH keys, run:

```shell
ssh-keygen -t ed25519 -C "your_email@example.com"
```

#### CI pipeline: deploy
As I mentioned earlier, we are using GitHub Actions, so let's look at the whole
deployment config:

_.github/workflows/deploy.yaml_
```yaml
name: Deploy

on:
  push:
    branches: [ master ]

jobs:
  checks:
    uses: ./.github/workflows/checks.yaml

  deploy:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    timeout-minutes: 20
    needs: [ checks ]
    steps:
      - uses: actions/checkout@v4
      - uses: jdx/mise-action@v2
      - uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}

      - name: Setup Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Expose GitHub Runtime for cache
        uses: crazy-max/ghaction-github-runtime@v3

      - name: Install kamal
        run: gem install kamal -v 1.5.2

      - name: Push env vars
        env:
          SERVER_IP: ${{ secrets.SERVER_IP }}
          REGISTRY_USERNAME: ${{ github.repository_owner }}
          REGISTRY_PASSWORD: ${{ github.token }}
          TRAEFIK_ACME_EMAIL: ${{ secrets.TRAEFIK_ACME_EMAIL }}
          APP_DOMAIN: ${{ secrets.APP_DOMAIN }}
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
          POSTGRES_DB: ${{ secrets.POSTGRES_DB }}
          POSTGRES_USER: ${{ secrets.POSTGRES_USER }}
          POSTGRES_PASSWORD: ${{ secrets.POSTGRES_PASSWORD }}
        run: kamal envify

      - name: Build and push
        run: |
          kamal registry login
          kamal build push --version=${{ github.sha }}

      - name: Migrations
        run:  |
          kamal build pull --version=${{ github.sha }}
          kamal app exec --version=${{ github.sha }} 'java -jar standalone.jar migrations'

      - name: Deploy
        run: kamal deploy --skip-push --version=${{ github.sha }}

      - name: Kamal Release
        if: ${{ cancelled() }}
        run: kamal lock release
```

Before the deployment we run pipeline with checking linting, formatting, 
outdated dependencies and tests:

```yaml
jobs:
  checks:
    uses: ./.github/workflows/checks.yaml
```
We will see it in detail in following section.

We need to give permissions to push Docker images to the ghcr.io registry:
```yaml
jobs:
  deploy:
    ...
    permissions:
      contents: read
      packages: write
```

Just general protection let's limit our pipeline by 20 minutes:

```yaml
jobs:
  deploy:
    ...
    timeout-minutes: 20
```

Checks must be completed successfully before deployment:

```yaml
jobs:
  deploy:
    ...
    needs: [ checks ]
```

We use step `- uses: jdx/mise-action@v2` to install Ruby. It's cached in the first run, 
so usually this step should be quick.   

To perform Kamal commands on the server we need to establish SSH connection:

```yaml
jobs:
  deploy:
    steps:
      ...
      - uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}
```

Then we have a couple of steps for enabling Docker cache and installing Kamal.

Next we need to push env variables to the server using `kamal envify`. 
So before pushing to master you have to set up secrets in the repo 
settings on the GitHub as described in the section "CI pipeline: environment variables" above. 

We split `kamal deploy` command into two steps, because we need 
to run database migrations from CI worker **before** deploying new application version.
So, for this purpose we use `--version` argument for each deployment command.

Just build Docker image and push to registry:
```shell
kamal build push --version=${{ github.sha }}
```

Pull built image on previous step and run migrations in it:

```shell
kamal build pull --version=${{ github.sha }}
kamal app exec --version=${{ github.sha }} 'java -jar standalone.jar migrations'
```

Perform actual deploy of the application, but do not build an image 
using `--skip-push` argument,  because we already built and pushed an image:

```shell
kamal deploy --skip-push --version=${{ github.sha }}
```

That's it. The last step is protection against failed deployments, 
we release lock to be able performing subsequent deployments:

```shell
kamal lock release
```

#### CI pipeline: checks 

The full checks pipeline that we run on each pull-request and push to master looks like:

_.github/workflows/deploy.yaml_
```yaml
name: Checks

on:
  pull_request:
    branches: [ master ]
  workflow_call:

jobs:
  deps:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: jdx/mise-action@v2
      - name: Cache Clojure dev dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-clojure-dev-${{ hashFiles('**/deps.edn') }}
          restore-keys: ${{ runner.os }}-clojure-dev
      - name: Install Clojure dev deps
        run: task deps

  fmt:
    runs-on: ubuntu-latest
    needs: [ deps ]
    steps:
      - uses: actions/checkout@v4
      - uses: jdx/mise-action@v2
      - name: Fmt
        run: task fmt-check

  lint:
    runs-on: ubuntu-latest
    needs: [ deps ]
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/restore-deps
      - name: Lint
        run: task lint-init && task lint

  outdated:
    runs-on: ubuntu-latest
    needs: [ deps ]
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/restore-deps
      - name: Outdated deps
        run: task outdated-check

  tests:
    runs-on: ubuntu-latest
    needs: [ deps ]
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/restore-deps
      - name: Run tests
        run: task test
```

I'm not going to stop here too long, because the config is pretty self-descriptive.
At the first step `deps` we install and cache all system dependencies by `uses: jdx/mise-action@v2`.
The `.tool-versions` looks like:
```
task 3.34.1
java temurin-21.0.2+13.0.LTS
clojure 1.11.3.1456
node 20.13.1
cljstyle 0.16.626
clj-kondo 2024.05.24
ruby 3.3.0
```
So, we use the same config during development and in CI pipeline. 

Then we set up cache for Clojure deps and install them:

```yaml
jobs:
  deps:
    steps:
      ...
      - name: Cache Clojure dev dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-clojure-dev-${{ hashFiles('**/deps.edn') }}
          restore-keys: ${{ runner.os }}-clojure-dev
      - name: Install Clojure dev deps
        run: task deps
```

After this step we run steps `lint`, `fmt`, `outdated`, `tests` in parallel 
with using cache of Clojure deps from previous step. All commands are described 
in Taskfile.yaml:

_Taskfile.yaml_
```yaml
tasks:
  ...
  test:
    desc: Run tests
    cmds:
      - clojure -X:dev:cljs:test
      
  fmt:
    desc: Fix code formatting
    cmds:
      - cljstyle fix --report {{ .DIRS }}

  lint-init:
    desc: Linting project's classpath
    cmds:
      - clj-kondo --parallel --dependencies --copy-configs --lint {{ .DIRS }}
    vars:
      DIRS:
        sh: clojure -Spath

  lint:
    desc: Linting project's code
    cmds:
      - clj-kondo --parallel --lint {{ .DIRS }}

  outdated-check:
    desc: Check outdated deps versions
    cmds:
      - clojure -M:outdated {{ .CLI_ARGS }}
  ...
```

### Summary

I quite like the approach and the simplicity that Kamal gives us for deployment. 
It's transparent and gives us ability to change almost any configuration of services.
Would be good to have a single binary instead of installing with Ruby and that staff. 
Also, I would avoid SSH connection from CI worker to server, but this is probably 
a sane compromise with simplicity of the setup.

Possible improvements of overall app installation that are out of scope of this article: 
- periodic database backup (for example, by using [`postgres-backup-s3`](https://github.com/eeshugerman/postgres-backup-s3?ref=luizkowalski.net) or similar);
- CDN for static files;
- collecting metrics and logs;
- using database-as-a-service instead of running our own.

The scope of this article is a bit wider than I planned initially. 
And I probably covered some important parts briefly or some didn't at all.
I tried to keep a right balance between project setup and deployment process 
with the main focus on the latter. Overall, I'm happy to share kind of 
a complete solution to set up and run a full-stack Clojure application. 
Hope it will be helpful and useful as-is or at least as an inspiration 
for your own project setup and deployment!
