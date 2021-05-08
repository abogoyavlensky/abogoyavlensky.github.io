A little bit later but I would like to announce that since 
version [`>= 1.2.2`](https://github.com/cloverage/cloverage/blob/master/CHANGELOG.md#122) 
[`Cloverage`](https://github.com/cloverage/cloverage) has built-in support 
for running tests using `Eftest` Clojure test runner. 
So now it is possible to have test coverage using `Eftest`.

Now I'm going to show some examples of usage and configuration options at the moment.

### Running using [tools.deps](https://clojure.org/guides/deps_and_cli)

All we need is to set dependencies and configure runner as a `:eftest` 
and optionally pass `Eftest`'s params as a `:runner-opts`:

```clojure
{...
 :aliases
 {...
  :test {:extra-paths ["test"]
         :extra-deps {eftest/eftest {:mvn/version "0.5.9"}
                      cloverage/cloverage {:mvn/version "1.2.2"}}
         :exec-fn     cloverage.coverage/run-project
         :exec-args {:test-ns-path ["test"]
                     :src-ns-path ["src"]
                     :runner :eftest
                     :runner-opts {:fail-fast? true}}}}
```

Then run:

```clojure
clojure -X:test
```

***Note**: in the case of using `tools.deps` we should explicitly define 
test and source paths for now.*

Example output could look like:

```bash
clojure -X:test

=> Running tests...
Loading namespaces:  (myprj.util.db myprj.util.test myprj.util.file myprj.util.spec myprj.models myprj.actions myprj.util.map myprj.schema myprj.sql myprj.migrations myprj.core)
Test namespaces:  (myprj.migrations-test myprj.models-test myprj.schema-test myprj.testing-config)
Instrumented myprj.util.db
Instrumented myprj.util.test
Instrumented myprj.util.file
Instrumented myprj.util.spec
Instrumented myprj.models
Instrumented myprj.actions
Instrumented myprj.util.map
Instrumented myprj.schema
Instrumented myprj.sql
Instrumented myprj.migrations
Instrumented myprj.core
Instrumented 11 namespaces in 1.7 seconds.

35/35   100% [==================================================]  ETA: 00:00

Ran 35 tests in 0.871 seconds
53 assertions, 0 failures, 0 errors.
Ran tests.
Writing HTML report to: /home/username/Projects/myprj/target/coverage/index.html

|------------------+---------+---------|
|        Namespace | % Forms | % Lines |
|------------------+---------+---------|
|    myprj.actions |   79.36 |  100.00 |
|       myprj.core |   83.50 |   87.50 |
| myprj.migrations |   86.36 |   94.83 |
|     myprj.models |   69.09 |  100.00 |
|     myprj.schema |  100.00 |  100.00 |
|        myprj.sql |   83.85 |   95.48 |
|    myprj.util.db |   91.60 |   94.44 |
|  myprj.util.file |   85.19 |   88.89 |
|   myprj.util.map |  100.00 |  100.00 |
|  myprj.util.spec |   42.31 |   71.43 |
|  myprj.util.test |   92.38 |  100.00 |
|------------------+---------+---------|
|        ALL FILES |   80.57 |   96.15 |
|------------------+---------+---------|
```

#### Caveates:
- at the moment for tools.deps it is not possible to configure regex options such as `:ns-exclude-regex`
or `:test-ns-regex` cause there is no ability to define regex patterns in EDN;

***Note**: regex support and requiring a definition of paths is general 
problem and should be fixed when `Cloverage` receives full `tools.deps` support.*


### Running using Leningen

The same ability availables in `Leningen` even with the full support of all options 
and not need to define test and source paths:

```
(defproject myprj "0.1.0-SNAPSHOT"
  ...
  :profiles {:coverage {:dependencies [[eftest "0.5.9"]
                                       [cloverage "1.2.2"]]
             :plugins [[lein-cloverage "1.2.2"]]
             :cloverage {:runner :eftest
                         :runner-opts {:fail-fast? true}
                         :ns-exclude-regex [#".*.core"  #"user"]}}}})
```

And run:

```
lein with-profiles +coverage cloverage
```
