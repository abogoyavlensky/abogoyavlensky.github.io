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
It has backend and frontend parts, the imporant parts of the project's structure
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


### Project setup: backend part

### Project setup: frontend part

### Deploy: config



### Deploy: initial deployment 

### CI/CD

### Manage app on a server

### Summary

