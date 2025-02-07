Introducing [Slim](https://github.com/abogoyavlensky/slim): a new build tool that simplifies building Clojure projects. Slim is designed to streamline your Clojure development, whether you’re creating an uberjar for your application or deploying a library. With zero ceremony and minimal configuration using your existing `deps.edn`, you can focus on writing code rather than managing build scripts.

### Minimal App Example

Add slim to your `deps.edn`:

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:mvn/version "0.2.1"}}
         :ns-default slim.app
         :exec-args {:main-ns my-app.core}}}}
```

Run your build:

```shell
clojure -T:slim build
```

Your uberjar will be created at `target/standalone.jar`.

### Minimal Library Example with Extended Metadata

*deps.edn*

```clojure
{:aliases
 {:slim {:deps {io.github.abogoyavlensky/slim {:mvn/version "0.2.1"}
                slipset/deps-deploy {:mvn/version "0.2.2"}}
         :ns-default slim.lib
         :exec-args {:lib my-org/my-lib
                     :version "0.1.0"
                     :url "https://github.com/my-org/my-lib"
                     :description "My awesome library"
                     :developer "Your Name"}}}}
```

Install your library locally:

```shell
clojure -T:slim install
```

Deploy snapshot:

```shell
clojure -T:slim deploy :snapshot true
```

Or publish a release version:

```shell
clojure -T:slim deploy
```

An example of using Slim in a Clojure project with a complete CI build setup can be found in the [Automigrate](https://github.com/abogoyavlensky/automigrate/blob/b9d0034effa0803ac2b3b47bf8c4ed119d2358ac/deps.edn#L36-L43) project.

Slim makes building and deploying Clojure projects effortless. For more information, check out the tool’s [documentation](https://github.com/abogoyavlensky/slim?tab=readme-ov-file#slim) and give it a try!
