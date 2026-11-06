Recently, I've been building [clj-pulse](https://github.com/abogoyavlensky/clj-pulse), a language server for Clojure. It is at a point where I use it every day, and I'd like to share it. This post explains what it is and how it works.

I've always wanted a faster language server for Clojure, one where navigation works right after I open the editor. Lately it matters even more: the way I work is changing, and I read much more code than I write by hand. When I open a project to check something, I want to jump between definitions right away, not wait for the analysis to finish. That was the main reason I started clj-pulse. I also wanted built-in Integrant support, but that is more of a convenience.

### TL;DR

clj-pulse makes a large project navigable about three seconds after you open it, and its dependencies a couple of seconds later, using about 550 MiB of memory. A regular-sized project is indexed almost instantly, and navigation is available right away. It works with any editor that has an LSP client. It is a native binary, and it indexes in stages, so you can navigate before the work is done. It does not do everything clojure-lsp does yet, and the comparison is below.

### The idea: answer first, finish later

The usual approach is to analyze the whole classpath before answering anything. [clojure-lsp](https://clojure-lsp.io) does it through [clj-kondo](https://github.com/clj-kondo/clj-kondo). The analysis is thorough, but you pay for it up front.

clj-pulse is a native binary. It does the work in stages, and navigation is available after each one:

1. **Project sources.** Every file under the project's `:paths` is parsed with [tree-sitter](https://tree-sitter.github.io/tree-sitter/), in parallel, and its definitions and usages go into an in-memory index. After this step you can jump around your own code.
2. **Dependencies from the cache.** The classpath is read from `.cpcache`, which the Clojure CLI has already written, so no JVM starts. Each JAR is indexed once and the result is cached on disk, keyed by the JAR's modification time. After this step you can jump into libraries.
3. **Classpath command.** `clojure -Spath` (or `lein classpath`) runs in the background, in case the cached classpath is stale. If it fails, the result of step 2 stays.
4. **clj-kondo, last.** If clj-kondo is installed, clj-pulse warms its dependency cache and hands linting over to it. Until then, five built-in lints give diagnostics. Nothing waits for it.

The rule I try to keep is: never a JVM on the hot path. Startup, keystrokes, and requests do not touch one. The classpath command in step 3 may start a JVM, but it runs in the background, and nothing waits for it either.

### The numbers

Here is clj-pulse 0.5.7 next to clojure-lsp 2026.07.06 on a large real project, [Metabase](https://github.com/metabase/metabase) (1,400+ files). Both run at their defaults on a MacBook Pro M1 Pro with 16 GB, with clj-kondo installed. Cold is the first open of a fresh checkout, a single run. Warm is the next open, the median of three runs.

| Metric | clj-pulse cold | clj-pulse warm | clojure-lsp cold | clojure-lsp warm |
|---|---|---|---|---|
| First navigation | 2.9 s | 1.8 s | 108 s | 24 s |
| All dependencies navigable | 4.5 s | 1.9 s | 108 s | 24 s |
| clj-kondo linting available | 24 s | 16 s | 108 s | 24 s |
| Memory once settled | 555 MiB | 530 MiB | 2,747 MiB | 1,772 MiB |
| Definition (median of 20) | < 1 ms | < 1 ms | 6 ms | 3 ms |
| Keystroke to diagnostics, 251 KiB file | 259 ms | 257 ms | 346 ms | 388 ms |

Each startup row is the time until a real `textDocument/definition` request returns the right location. The two servers do different work in that time. clojure-lsp analyzes the whole classpath before it answers anything, so its three startup rows are one moment, and by then it knows more about your code. clj-pulse makes each stage available as soon as it is done. Linting with clj-kondo is not something you wait for: navigation, completion, and the built-in lints work the whole time. The keystroke row includes the wait each server takes after an edit before linting it: 50 ms for clj-pulse, 75 ms for clojure-lsp.

On a medium project, [clj-kondo](https://github.com/clj-kondo/clj-kondo) itself (400 files), the picture is the same at a smaller scale: clj-pulse navigates after **688 ms** cold and **213 ms** warm, against 7.8 s and 1.2 s, and settles at 98 MiB of memory, against 286 MiB. The full tables for both projects, the method, and how to reproduce them with `bb bench` are in the [performance doc](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/PERFORMANCE.md).

These numbers describe two projects on one machine. Yours will differ.

### What you get

The everyday set is there: go to definition into project code and library JARs, fuzzy completion with auto-require, hover, signature help, references, rename, symbol search, and namespace quickfixes.

A few things I added because I needed them:

- **[Integrant](https://github.com/weavejester/integrant) navigation.** Jump from a component key in `config.edn`, or from an `#ig/ref`, to its `ig/init-key`. References and rename work on qualified keywords across sources and configs.
- **Monorepos.** Every `deps.edn`, `project.clj`, or `lgx.edn` project in the workspace is found and indexed, with classpath settings per project.
- **[let-go](https://github.com/nooga/let-go).** `.lg` files and [lgx](https://github.com/abogoyavlensky/lgx) dependencies get the same navigation.

The full list is in the [feature reference](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/FEATURES.md).

### How it compares to clojure-lsp

clojure-lsp is mature and does more. Here is where clj-pulse stands against it today:

- **Everyday navigation and editing are on par**, and faster.
- **Most refactorings are missing.** Extract function, inline symbol, introduce let, and the rest are not there yet. Neither are go to implementation, code lens, or updating the `ns` form when you rename a file; those three are planned next. Paredit-style editing is left to the editor.
- **Java support is partial.** JDK classes, static members, and constructors work. Instance methods, library classes, and decompilation do not yet.
- **The server does not format documents yet.** It indents on Enter, and the editor does the rest: [Clojure Pulse](https://github.com/abogoyavlensky/clojure-pulse-vscode) bundles cljfmt, and Calva has its own formatter.
- **It reads syntax, it does not analyze.** clojure-lsp builds on clj-kondo's analysis. clj-pulse parses the code and knows what `defn`, `let`, and destructuring bind because it implements those rules itself. A macro that defines things is recognized through a built-in table, your clj-kondo `:lint-as` config, or its name (`mu/defn` defines what `defn` defines). An unusual macro it has never seen is just a call.

To find where the syntactic approach goes wrong, I check it against clj-kondo's own analysis. On the clj-kondo codebase, I ask the server 5,032 definition, references, and rename questions whose answers the analysis already knows, and 4,754 of them (about 94%) agree today. The rest is a list I'm working through.

The feature-by-feature list is in the [parity matrix](https://github.com/abogoyavlensky/clj-pulse/blob/master/docs/PARITY.md).

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

For the full linter set, [install clj-kondo](https://github.com/clj-kondo/clj-kondo/blob/master/doc/install.md). clj-pulse finds it on its own. Create a `.clj-kondo` directory in each project to enable clj-kondo's cross-file cache.

### Wrapping up

clj-pulse is at 0.5.x. It is my daily driver, and it is tested end to end through real editors on every change, but expect gaps. If you open a real project with it and something is wrong, slow, or missing, please [open an issue](https://github.com/abogoyavlensky/clj-pulse/issues). Hope you enjoy using it!
