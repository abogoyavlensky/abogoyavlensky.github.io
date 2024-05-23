### TL;DR

To get a quick example of a Clojure app setup with full deployment configuration, 
check out the [clojure-kamal-example](https://github.com/abogoyavlensky/clojure-kamal-example) 
project. To try it yourself, simply clone the repo and locally execute 
the commands from the [Deploy: summary](https://github.com/abogoyavlensky/clojure-kamal-example/tree/master?tab=readme-ov-file#deploy-summary)
section. For a detailed explanation of each step, including Clojure/Script app setup, 
deployment and CI/CD, please keep reading. If you already have a Docker-container 
with an exposed `80` port, you can jump right to the "Deployment" 
section of this article.

### Overview

Sometimes you need to quickly deploy an app without worrying 
too much about scalability. In this case, it's fine to deploy an app on
a single server. This approach works for small/medium non-critical apps,
staging, or preview environments. In this article, 
I'm going to show how we can configure Kamal to deploy 
a Clojure/Script app on a single server with a TLS certificate and the 
ability to run multiple apps. The good part is that when the time comes, 
you can add a load balancer in front of the app and easily scale 
to multiple servers.

I'm going to start by highlighting the important parts of 
the application setup from my perspective. Then we will overview 
the deployment config, perform server setup, and conduct the first deployment.
We will also check how we can manage our app on the server. 
Finally, we will configure a full CI/CD process to automatically deploy 
our app to the server from GitHub Actions.  
