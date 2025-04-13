In this article, I will show you how to build a Clojure library and publish it to Clojars with minimal effort, time and configuration.
We are going to use Slim, a build tool that I released recently and that simplifies building Clojure projects.

### Publishing an existing library

If you have an existing library and want to publish it to Clojars, you can use Slim to do it with minimal effort.
Assuming you have a `deps.edn` file in your project root, you can add Slim to your `deps.edn` file:

```clojure
{:aliases
 {:build {:deps {io.github.abogoyavlensky/slim {:mvn/version "0.3.2"}
                 slipset/deps-deploy {:mvn/version "0.2.2"}}
          :ns-default slim.lib
          :exec-args {:version "0.1.0"
                      :lib io.github.githubusername/libraryname
                      :url "https://github.com/githubusername/libraryname"
                      :description "Library description"
                      :developer "Your Name"}}}}
```

And that's it! You can now set up environment variables `CLOJARS_USERNAME` and `CLOJARS_PASSWORD`, 
and then run `clojure -T:build deploy` to publish your library to Clojars.

If you want to publish a snapshot version first, you can run `clojure -T:build deploy :snapshot true`.
There are some more options available, you can check them in the [documentation](https://github.com/abogoyavlensky/slim?tab=readme-ov-file#slim).

### Publishing a new library

What if you have just an idea for a new library and want to implement it and publish to Clojars?
Recently, I've built a couple of small libraries and decided to create a new template to speed up the process.

Meet [clojure-lib-template](https://github.com/abogoyavlensky/clojure-lib-template): a template for creating a new Clojure library.
It has few advantages: 
- Minimalistic and easy to understand
- Built-in GitHub Actions workflows for CI/CD with publishing to Clojars
- Comprehensive development tooling setup (linting, formatting, deps versions, testing)
- Preconfigured build and deployment to Clojars using Slim
- MIT License by default

#### Example library idea

Just for an example, let's say we want to implement a library with the only one function that finds an empty port on the local machine for a server.
We can call it `freeport`.

#### Creating a new project

We need to create a project structure first. If don't have yet a deps-new installed you can install it with:

```shell
clojure -Ttools install-latest :lib io.github.seancorfield/deps-new :as new
```

Then let's create a new project:

```shell
clojure -Sdeps '{:override-deps {org.clojure/clojure {:mvn/version "1.12.0"}}}' -Tnew create :template io.github.abogoyavlensky/clojure-lib-template :name io.github.yourusername/freeport
```

*Note: change `yourusername` to your own GitHub username, or the whole prefix if you are going to host your project on a different service.*

If you already have Clojure version `1.12.x` installed on your machine you can skip the first argument `-Sdeps`, and run just:

```shell
clojure -Tnew create :template io.github.abogoyavlensky/clojure-lib-template :name io.github.yourusername/freeport
```

This command will create a new project with the name `freeport` in the current directory, with the structure:

```text
├── .clj-kondo/            # Clojure linting configuration
├── .github/               # GitHub Actions workflows and configurations
├── dev/                   # Development configuration directory
│   └── user.clj           # User-specific development configuration
├── src/                   # Source code directory
│   └── {{name}}           # Main namespace directory
│       └── core.clj       # Main namespace file
├── test/                  # Test files directory
│   └── {{name}}           # Test namespace directory
│       └── core_test.clj  # Test namespace file
├── .cljfmt.edn            # Formatting configuration
├── .gitignore             # Git ignore rules
├── .mise.toml             # mise-en-place configuration with system tools versions
├── bb.edn                 # Babashka tasks configuration
├── deps.edn               # Clojure dependencies and aliases
├── LICENSE                # License file
├── CHANGELOG.md           # Changelog file
└── README.md              # Project documentation
```

We can start with installing system dependencies:

```shell
mise trust && mise install
```

[mise-en-place](https://mise.jdx.dev/) is an optional tool that helps conveniently manage system tools versions.
You can skip it and install all dependencies manually, in this case consult `.mise.toml` file for the versions.

And then we can initiate git repository and add first commit:

```shell
git init
git add .
git commit -am 'Initial commit'
```

Then check linting, formatting, outdated dependencies and tests for the new project with:

```shell
bb check
```

Commit if anything was changed after formatting or checking dependencies.

#### Add implementation

Let's implement main logic for our library.

We can open `src/freeport/core.clj` file and add the following code:
```clojure
(ns freeport.core
  (:import (java.net ServerSocket)))

(defn get-freeport
  []
  (with-open [socket (ServerSocket. 0)] 
    (.getLocalPort socket)))
```

Now we can test it. Open `test/freeport/core_test.clj` file and add the following code:

```clojure
(ns freeport.core-test
  (:require [clojure.test :refer :all]
            [freeport.core :as core])
  (:import [java.net ServerSocket]))

(defn port-free? [port]
  (try
    (with-open [_socket (ServerSocket. port)]
      true)
    (catch Exception _
      false)))

(deftest test-port-is-free-ok
  (let [port (core/get-freeport)]
    (is (true? (port-free? port)))
    (is (<= 1024 port 65535))))
```

We've just tested that the port is actually free and we can use it, and that it is in the range of 1024-65535.
If we run tests and other checks again, it should pass: `bb check`.

At this point we can commit our changes:
```shell
git commit -am 'Add freeport implementation'
```

#### Publishing to Clojars

Most likely you will want to change the name of the library to your own.
You will also need to change the `:description` and the `:developer` name in the `deps.edn` file:

```clojure
{;...
 :aliases
 {:build {:deps {io.github.abogoyavlensky/slim {:mvn/version "0.3.2"}
                 slipset/deps-deploy {:mvn/version "0.2.2"}}
          :ns-default slim.lib
          :exec-args {:version "0.1.0"
                      :lib io.github.abogoyavlensky/freeport
                      :url "https://github.com/yourusername/freeport"
                      :description "TODO: Add description"
                      :developer "Your Name"}}}}
```

Then define Clojars credentials in the environment variables `CLOJARS_USERNAME` and `CLOJARS_PASSWORD`:

```shell
export CLOJARS_USERNAME=yourusername
export CLOJARS_PASSWORD=yourpassword
```

And finally run `bb deploy-snapshot` to publish your library snapshot version first to Clojars.

Now you can test the snapshot version in your project. If all looks good, you can run `bb deploy-release` to publish a release version.

#### Publish library from GitHub Actions

The library is implemented and deployed from local machine, but it would be even better if we could publish it from CI automatically.
The lib template already has GitHub Actions workflows configured for this purpose. All you need is just set up the environment variables `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` for Actions in the GitHub repository settings.

![Firewall config](/assets/images/articles/11_gh_actions_secrets.png)

The idea is to publish a snapshot version on every push to the main branch, and a release version on every tag push.
Now, if you push a new commit to the main branch, you will see the workflow running in the Actions tab and a couple of minutes later snapshot version will be published to Clojars.

As soon as you library reaches a new version, you can bump it in `deps.edn` at:

```clojure
{;...
 :aliases
 {:build {;...
          :exec-args {:version "0.1.1"
                      ;...
                      }}}}
```

After committing and pushing the changes to main branch we can create a new git tag. You can do it manually or with an existing command:


```shell
bb release
```

It's really just a shortcut for `git tag` and `git push`. As a result the new tag with latest version from `deps.edn` will be created and pushed to the remote repository. 

### Summary

In this article we've shown all stages of building and publishing an existing library with Slim and a new library from scratch using `clojure-lib-template`.
Hope you found it useful and it helps you with the next library you want to build!
