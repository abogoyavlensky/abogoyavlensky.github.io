Today I would like to show an approach to organize auto-formatting
on a Clojure project from a practical point of view.
I will describe the reasons behind choosing the tool and configuration 
for two different formatting styles.

### Requirements for formatting tool

In my experience having forced auto-formatting on a project leads
to keeping code in consistent good-looking shape, reduces some of redundant discussions about code style
during code review, and eventually better code understanding for a whole team.

To have all that benefits and not spend too much time every day to keep
it working the tool and approach, in general, should meet certain requirements.
For instance, we should be able to:

- run formatting locally on every commit or push - so it should be fairly fast to prevent consuming our time;
- run format checking in CI on every commit;
- have exactly same results in any environment;
- configure formatting for a new project in a short time as possible.

And it would be great if we will have the same commands and configurations to run it locally and in CI 
to get same results in all environments.

### Existing tools

For now, as I see there are several mature tools for Clojure formatting which are:

- [`cljfmt`](https://github.com/weavejester/cljfmt) - a popular and really reliable choice;
- [`zprint`](https://github.com/kkinnear/zprint) - a bit less popular but is mature too even offer some more features.

`cljfmt` is really great I used to use it a lot, but it doesn't have a supported binary.
For me, it is one of the main points because I would like to format code often
without a need to keep REPL running.
`zprint` has binary but configuration seems a bit unintuitive to me.

### Cljstyle

There is one more tool that meets almost all my requirements: [`cljstle`](https://github.com/greglook/cljstyle).
Despite it is not so popular as tools described above It has mature
foundation cause based on `cljfmt` code. `cljstyle` has a little
different rules' logic but is not affect the quality of results.
It keeps all good parts of `cljfmt` and even adds a few more improvements.
Main of them are:

- standalone binary;
- can keep count of empty lines between blocks as you configure it (two by default);
- can break var's and function's definitions by lines;
- can remove trailing whitespaces and add new lines at the end of a file.

### About formatting rules

For the rule's configuration, we have several options.
First, keeping default rules which mostly follow
[`Clojure Style Guide`](https://github.com/bbatsov/clojure-style-guide) 
excepting single space indentation for several forms (which is configurable too).

It is a more widespread and almost standard approach for now. It's useful for large projects
which partially already have a near style like that. 
Because it will be faster to add rules with minor changes of a legacy codebase.
Or for those who just like that formatting style.

But there is an alternative: *universal Clojure style
formatting*. which is described in the article ["Better Clojure formatting"](https://tonsky.me/blog/clojurefmt/).
I like the idea because it has a few rules, the same logic to treat
different forms (actually, there could be one exception: *imports*) and it is not broke
if once you will decide to add some new arbitrary macro.
Here are the rules:

- format with two space indentation every multi-line list starting with symbol;
- format regular multi-line other sequences by a first arg.

Possible exception for ns formatting (because it is too common):

- requires are indented by the first arg.

### Cljstyle rules for universal formatting

To change default behavior and configure `cljstyle` 
please place file `.cljstyle` to a root of a project with content:

```clojure
{:indents ^:replace {#"^:?require$" [[:block 0]]
                     #"^:?import$" [[:block 0]]
                     #"^:?use$" [[:block 0]]
                     #"^[^ \[]" [[:inner 0]]}
 :line-break-vars? false
 :rewrite-namespaces? false}
```

I turned off the line breaking for vars because sometimes it is convenient
to have var definition in the same line with its value.
Also, rewriting of namespaces has been switched off because it conflicts
with custom rules and it will be done with additional rules.

The key part is under `:indents` keyword. I replaced default indentation
rules by custom ones which as I supposed ideally
should not be changed (at least too often). Let's describe the idea shortly:

- `#"^[^ \[]" [[:inner 0]]` - enable two spaces indentation for all multi-line lists starting 
from any character except opening square bracket `[` - to fix case with multi-arity functions like:

  ```clojure
  (defn new-server
    ([app]
     (new-server app {}))
    ([app options]
     (map->Server {:app app
                   :options options)})))
  ```

- `#"^:?require$" [[:block 0]]` - other rules for `require`, `import` and `use` for formatting imports by first arg.
    ```clojure 
    (ns project.core
      (:require [clojure.string :as str]
                [clojure.xml :as xml]
                [compojure.route :as route]))
    ```

### Empty lines between code blocks

By default, `cljstyle` forces two empty lines between code blocks and forms.
I think it is reasonable because in my opinion, it improves readability a bit
and makes it easier for eyes to distinct different functions.

But you could disable empty line editing at all or choose some custom settings
for that. For example, if you prefer single empty line between code blocks just add
following lines to config map next to the last keyword:

```clojure
{...
 :max-consecutive-blank-lines 1
 :padding-lines 1}
```

### Run formatting

Let's try to run formatting. To do that we could download the prepared binary
from the [releases](https://github.com/greglook/cljstyle/releases) page and run:

```bash
cljstyle check --report src
```

The output could be something like:

```diff
--- a/src/project/server.clj
+++ b/src/project/server.clj
@@ -4,7 +4,7 @@


 (defrecord Server
-    [app options server]
+  [app options server]

   component/Lifecycle

Checked 8 files in 291 ms
     7 correct
     1 incorrect
Resulting diff has 2 lines
1 files formatted incorrectly
```

Now we can fix it:

```bash
cljstyle fix --report src

Checked 8 files in 279 ms
     8 correct
```

There are some cases when you want (or need) to run formatting in a docker container.
For example, in CI. Or to use a single way to run formatting locally by hand,
on git hook, and in CI. You can simply [build](https://github.com/abogoyavlensky/docker/blob/master/cljstyle/Dockerfile)
on your own or use the one I published on [`Dockerhub`](https://hub.docker.com/r/abogoyavlensky/cljstyle):

```bash
docker run -v $PWD:/app --rm abogoyavlensky/cljstyle cljstyle check --report src
```

Or with simple docker-compose file:

```yaml
version: "3.8"
services:
  fmt:
    image: abogoyavlensky/cljstyle
    command: cljstyle check --report src
    volumes:
      - .:/app
```

*Also, for convenience we could move command to `bash`-script or `Makefile` (with some logic for choosing fmt action)
to have single "source of true" for that and avoid duplication it.* 

### Caveats

The tool is still being evolving and has some minor drawbacks which seem not so critical to me.
I noticed the following:

- multi-line lists as data structure `'()` or `()` inside `case` formatted with two spaces;
  - *solution*: use if possible `(list ...)` or ignore expression with metadata: `^:cljstyle/ignore`;
- the formatter throw an exception when auto-resolve namespace for a map is used `#::{}`;
  - *solution*: use full qualified map name ` #:some-module{}`.

One thing yet which is not actually a downside, but sometimes I miss it:
 
- ability to keep strict line length and align code to it.

### Editors

In the official repository, there is a [page](https://github.com/greglook/cljstyle/blob/master/doc/integrations.md)
about using `cljstyle` with `vim` and `emacs`.
I will show simple configs to format a current file or the whole project
on a keypress in [`VS Code`](https://code.visualstudio.com/) and [`IDEA`](https://www.jetbrains.com/idea/).

#### VS Code

To add fmt task you could create file `.vscode/tasks.json` at the root of the project with content:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Run fmt",
      "type": "shell",
      "command": "cljstyle fix ${file}",
      "presentation": {
        "reveal": "silent"
      }
    }
  ]
}
```

If you would like format the whole project just replace `${file}` to list of dirs
whatever you want, for example: `cljstyle fix src test dev`.

Then you could add a shortcut to `keybindings.json`:

```json
    {
        "key": "<your key sequence>",
        "command": "workbench.action.tasks.runTask",
        "args": "Run fmt"
    }
```

#### IDEA

In `IDEA` we have an option to add external tool in `Settings -> Tools -> External tools -> +`.
Then configure the fmt tool as shown in the picture:

![IDEA fmt config](/assets/images/articles/idea_fmt_config.png)

As before you could replace `$FilePath$` to static dir list `src dev ...`. 
Then add appropriate key bindings in `Settings -> Keymap`.

### Check formatting on a git commit/push

The simplest possible way is to create file `.git/hooks/pre-commit` at the root of project dir containing:

```bash
#!/bin/bash
set -e

cljstyle fix --report src
```

Or you could use a bit more powerful tools like [`pre-commit`](https://pre-commit.com/) 
or [`Lefthook`](https://github.com/Arkweid/lefthook). An example for `Lefthook`:

```yaml
pre-commit:
  commands:
    fmt:
      glob: "*.{clj,cljs,cljc,edn}"
      run: cljstyle fix --report {staged_files}
```

###  Recap

So I described yet another tool for formatting Clojure code which gives an ability
to choose between default formatting and universal the same time with execution speed
in any environment. We didn't touch CI configs but the idea is similar and
you could pick any of CI systems and apply the fmt command there
using standalone binary or docker image. 

Thanks for reading!
