# clj-pulse: a Clojure language server that is ready in two seconds

For the last several months I've been building [clj-pulse](https://github.com/abogoyavlensky/clj-pulse), a language server for Clojure written in Rust. It is at a point where I use it every day, and I'd like more people to try it on real projects.

<!-- TODO(author): one or two sentences of personal motivation. What made you start: the wait on a specific project, memory on a laptop, let-go support? -->

The reason I started is simple: on a large project I open the editor and wait. With [clojure-lsp](https://clojure-lsp.io) on Metabase, the first go-to-definition answers after 106 seconds on a fresh checkout and after 28 seconds on every open after that, and the server settles at more than 2 GB of memory. clojure-lsp is a great tool and it does a lot during that time. I wanted to see how far a different tradeoff could go.

### TL;DR

clj-pulse makes a large project navigable about two seconds after you open it, dependencies included a couple of seconds later, in about 400 MiB of memory. It works with any editor that has an LSP client. It gets there by parsing instead of analyzing, and that has a cost, which I describe below.

### The idea: answer first, finish later

clojure-lsp analyzes the whole classpath through [clj-kondo](https://github.com/clj-kondo/clj-kondo) before it answers anything. The analysis is thorough, and you pay for it up front.

clj-pulse does the work in stages, and navigation is available after each one:

1. **Project sources.** Every file under the project's `:paths` is parsed with [tree-sitter](https://tree-sitter.github.io/tree-sitter/), in parallel, and its definitions and usages go into an in-memory index. After this step you can jump around your own code.
2. **Dependencies from the cache.** The classpath is read from `.cpcache`, which the Clojure CLI has already written, so no JVM starts. Each JAR is indexed once and the result is cached on disk, keyed by the JAR's modification time. After this step you can jump into libraries.
3. **Classpath command.** `clojure -Spath` (or `lein classpath`) runs in the background, in case the cached classpath is stale. If it fails, the result of step 2 stays.
4. **clj-kondo, last.** If clj-kondo is installed, it warms its cache and takes over linting. Until then, five built-in lints give diagnostics. Nothing waits for it.

The rule I try to keep is: never a JVM on the hot path. Startup, keystrokes, and requests do not touch one.

### The numbers

This is Metabase (1,400+ files) on a MacBook Pro M1 Pro with 16 GB, clj-pulse 0.5.4 against clojure-lsp 2026.07.06, both at their defaults with clj-kondo installed. Cold is the first open of a fresh checkout. Warm is the next open, as the median of three runs.

| Metric | clj-pulse cold | clj-pulse warm | clojure-lsp cold | clojure-lsp warm |
|---|---|---|---|---|
| First navigation | 2.1 s | 1.4 s | 106 s | 28 s |
| All dependencies navigable | 3.9 s | 1.6 s | 106 s | 28 s |
| clj-kondo finished | 25 s | 16 s | 106 s | 28 s |
| Memory once settled | 427 MiB | 412 MiB | 2,705 MiB | 2,151 MiB |
| Definition (median of 20) | 11 ms | 11 ms | 6 ms | 8 ms |

Both servers are driven by the same client through the same requests, and a startup row is the moment a real `textDocument/definition` lands where it should. You can reproduce it with `bb bench` in the repo. The method and a second, smaller project are in the [performance doc](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/PERFORMANCE.md).

These numbers describe one project and one machine. Yours will differ.

### What it costs

The table is not a free win, and I want to be clear about that:

- **It reads syntax, it does not analyze.** clj-pulse knows what `defn`, `let`, and destructuring bind because it implements those rules itself. A macro that defines things is recognized through a built-in table, your clj-kondo `:lint-as` config, or its name (`mu/defn` defines what `defn` defines). An unusual macro it has never seen is just a call.
- **A request is slower once both servers are settled.** clojure-lsp looks up an answer it stored at startup. clj-pulse re-reads the open buffer on each request. On the largest file in Metabase that is 11 ms against 6-8 ms. On a small file both take about 2 ms.
- **Diagnostics from clj-kondo are slower per keystroke.** clojure-lsp embeds clj-kondo, clj-pulse starts a process: about 570 ms against 420 ms on a 251 KiB file.
- **Java support is partial.** JDK classes, static members, and constructors work. Instance methods, library classes, and decompilation do not yet.
- **There are fewer refactorings.** Rename, add-require, and clean-ns are there. The long list clojure-lsp has is not.

To know where the syntactic approach is wrong, I check it against clj-kondo's own analysis. On the clj-kondo codebase, the server is asked 5,249 definition, references, and rename questions that the analysis knows the answer to, and 4,920 of them agree today. The rest is a list I work through.

### What you get

The everyday set is there: go to definition into project code and library JARs, fuzzy completion with auto-require, hover, signature help, references, rename, symbol search, and namespace quickfixes.

A few things I added because I needed them:

- **[Integrant](https://github.com/weavejester/integrant) navigation.** Jump from a component key in `config.edn`, or from an `#ig/ref`, to its `ig/init-key`. References and rename work on qualified keywords across sources and configs.
- **Monorepos.** Every `deps.edn`, `project.clj`, or `lgx.edn` project in the workspace is found and indexed, with classpath settings per project.
- **[let-go](https://github.com/nooga/let-go).** `.lg` files and [lgx](https://github.com/abogoyavlensky/lgx) dependencies get the same navigation.

The full list is in the [feature reference](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/FEATURES.md).

### Try it

Install the server on macOS or Linux with Homebrew:

```sh
brew install abogoyavlensky/tap/clj-pulse
```

Or with mise:

```sh
mise use -g github:abogoyavlensky/clj-pulse
```

It speaks plain LSP over stdio, so any editor with an LSP client can run it. The ones I test on every change:

- **VS Code with [Calva](https://calva.io/).** Keep everything you have and change one setting:

  ```json
  {
    "calva.clojureLspPath": "/path/to/clj-pulse"
  }
  ```

- **VS Code with [Clojure Pulse](https://github.com/abogoyavlensky/clojure-pulse-vscode).** My own extension, with the server bundled. It deserves a separate post.
- **Neovim 0.11+** with the built-in LSP client, no plugin needed.

Zed works for project navigation, but not for library JARs yet. The config for each editor is in the [editor setup guide](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/EDITORS.md).

For the full linter set, [install clj-kondo](https://github.com/clj-kondo/clj-kondo/blob/master/doc/install.md). clj-pulse finds it on its own.

### Wrapping up

clj-pulse is 0.5.x. It is my daily driver, and it is tested end to end through real editors on every change, but expect gaps. If you open a real project with it and something is wrong, slow, or missing, please [open an issue](https://github.com/abogoyavlensky/clj-pulse/issues). That is the most useful thing you can do for the project right now.
