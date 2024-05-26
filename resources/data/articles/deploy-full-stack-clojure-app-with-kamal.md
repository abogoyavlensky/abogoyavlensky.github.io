### TL;DR

To get a quick example of a Clojure app setup with full deployment configuration, 
check out the [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example) 
project. To try deployment yourself, simply clone the repo and locally execute 
the commands from the [Deploy: summary](https://github.com/abogoyavlensky/clojure-kamal-example/tree/master?tab=readme-ov-file#deploy-summary)
section. If you already have a Docker image that exposes port 80
you can skip project setup part and go straight to "Deployment config" section fo this article.

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

You can check the example project in the repository [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example).
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

This is Clojure web app with PostgreSQL as a main database. 
Here I use general names `api`, `ui`, `common` for namespace prefix of each application part.
Speaking about libraries and tools, we are using: [Integrant](https://github.com/weavejester/integrant) for app system management, 
[Reitit](https://github.com/metosin/reitit) for routing on the backend and frontend, 
[Malli](https://github.com/metosin/malli) for data validation, [Automigrate](https://github.com/abogoyavlensky/automigrate) for managing database migrations. 
On the frontend we are using ClojureScript with [Re-frame](https://github.com/day8/re-frame), [Shadow CLJS](https://github.com/thheller/shadow-cljs) as a build system, and [Tailwind CSS](https://tailwindcss.com/) for styling.
For managing app locally and in CI we use [Taskfile](https://taskfile.dev/) as a replacement for Make and [mise-en-place](https://mise.jdx.dev/) for tools version management.

For demonstration purposes I added a couple of database [models](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/resources/db/models.edn) `movie` and `director`, an API route 
to get all records from the `movie` model and the representation of that list on the web page.
API routes are defined in the common cljc-directory to get the opportunity to use 
API routes on frontend by names from single source of truth. 

#### Backend part

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

System contains three components: `:api.db/db` - database connection pool,
`:api.handler/handler` - application handler with API Reitit-router based on ring and middlewares,
`:api.server/server` - Jetty server. I like the approach to group component config options into `:options` key 
to do not mix them with references to other components.

Here we use [Aero](https://github.com/juxt/aero) to extend system config with handy data readers.
There is a `#profile` reader to switch between `dev`, `test` and `prod`; 
`#env` for reading environment variables. It is extended with Integrant `#ig/ref` to use components 
as references in other components. Also, I added `#free-port` to pick free port for
API web server while it's starting in tests. 

In [handler](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/src/clj/api/handler.clj) there are options to enable auto-reloading backend code on any change without restarting the whole system
by using `:reloaded?` and `:cache-assets?` to enable static assets cashing in production. 
You read about my approach for auto-reloading in the another [article](https://bogoyavlensky.com/blog/auto-reloading-ring/). 

During tests we automatically start database as a Docker container using Testcontainers's
feature [JDBC support](https://java.testcontainers.org/modules/databases/jdbc/#using-postgresql).
All we need is to add `tc:` prefix after `jdbc:` in the JDBC URL 
and management database container will be done by Testcontainers under the hood.
To speed up tests we use `TC_DAEMON=true` parameter to JDBC URL to reuse the same
container for multiple tests.

#### Frontend part

The frontend setup is pretty minimal yet powerful. We use `reitit.frontend.easy/start!` to configure a router on the frontend.

TODO:!!!

### Build docker image

I tried to make Dockerfile as simple and transparent as possible,
so I don't use Taskfile and mise here. I use alpine as a base image. 
There are two stages image: 1 stage - build of uberjar with all minified and hashed frontend static files;
2 stage - final image with just the prepared uberjar from the previous stage.  

```dockerfile
FROM --platform=linux/amd64 clojure:temurin-21-tools-deps-1.11.3.1456-alpine AS build

WORKDIR /app

# Install npm
RUN echo "http://dl-cdn.alpinelinux.org/alpine/v3.20/community" >> /etc/apk/repositories
RUN apk add --update --no-cache npm=10.8.0-r0

## Node deps
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

Unfortunately official Clojure Docker image doesn't support arm64 platform, 
so if you are going to deploy first time from macOS we need to add `--platform=linux/amd64`
to the `FORM` definition.

At build step we run css, js and uberjar builds separately one by one. 
I'm going to publish images to GitHub ghcr registry, so it's convenient to 
link uploaded images with repository by default for this purpose I added `LABEL`
to the final image definition. I added `-Xmx256m` option to java command, 
as I would like to deploy to a small instance, you can extend or remove this config
as you prefer.

### Deployment config

Let's look at full deployment config for Kamal that includes traefik, web app and database accessory configuration.

_deploy.yaml_
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
     publish:
       - "6433:5432"
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



### Initial deployment 

### CI/CD

### Manage app on a server

### Summary

