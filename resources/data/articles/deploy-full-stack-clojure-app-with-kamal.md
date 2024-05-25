### TL;DR

To get a quick example of a Clojure app setup with full deployment configuration, 
check out the [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example) 
project. To try deployment yourself, simply clone the repo and locally execute 
the commands from the [Deploy: summary](https://github.com/abogoyavlensky/clojure-kamal-example/tree/master?tab=readme-ov-file#deploy-summary)
section.

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

For demonstration purposes I added a couple of database [models](https://github.com/abogoyavlensky/clojure-kamal-example/blob/master/resources/db/models.edn), an API route 
to get all records from the main model and the representation of that list on the web page.
API routes are defined in the common cljc-directory to get the opportunity to use 
API routes on frontend by names from single source of truth. 

The whole backend app system looks like:

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

In handler there are options to enable auto-reloading backend code on any change without restarting the whole system
by using `:reloaded?` and `:cache-assets?` to enable static assets cashing in production. 
You read about my approach for auto-reloading in the another [article](https://bogoyavlensky.com/blog/auto-reloading-ring/). 

During tests we automatically start database as a Docker container using Testcontainers's
feature [JDBC support](https://java.testcontainers.org/modules/databases/jdbc/#using-postgresql).
All we need is to add `tc:` prefix after `jdbc:` in the JDBC URL 
and management database container will be done ny Testcontainers under the hood.
To speed up tests I use `TC_DAEMON=true` parameter to JDBC URL to reuse the same
container for multiple tests.



### Project setup: backend part

### Project setup: frontend part

### Deploy: config



### Deploy: initial deployment 

### CI/CD

### Manage app on a server

### Summary

