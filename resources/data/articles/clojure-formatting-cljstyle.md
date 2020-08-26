Today I would like to show my approach to organize auto-formatting on a Clojure project from a practical point of view. 
I will describe the reasons for choosing the tool. 
And I'm going to explain how we could use it to add common or universal formatting rules configuration to a project.

### Requirements for formatting tool

In my experience forced auto-formatting on a project leads to keeping code in good looking shape,
reduces most of the redundant discussions during code review, and eventually better code understanding for a whole team. 
 
But to have all that benefits and not spend too much time every day to keep it working the tool and approach, in general, 
should meet certain requirements. We should be able to:

- run formatting locally on every commit or push - so it should be fairly fast to prevent consuming our time;
- run formatting in CI on every commit;
- configure formatting for a new project in a short time a possible;
- customize formatting rules per project if we need it;
- add to existing project step by step;

And it would be great if we will have the same commands and configurations to run it locally and in CI.

### Existing tools

As I see there are several mature tools for Clojure formatting which are:

- cljfmt - a popular and really reliable choice;
- zprint - a bit less popular but is mature too despite sophisticated rule configuration.

cljfmt is really great but doesn't have an official binary. For me, it is one of the main points because I would like to run it often. 
zprint has supported binary but configuration seems a bit unintuitive to me.

### Cljstyle

There is one more tool that meets almost all my requirements. 
Despite it is not so popular as tools described above It has mature foundation cause based on cljfmt code base. 
cljstyle has a little bit different rules' logic but is not affect the quality of results. 
It keeps all good parts of cljfmt and adds a few more improvements. Main of them are:

- standalone binary;
- no single space indentation except sequences and you can control it;
- can keep count of empty lines between blocks as you configure it (two by default);
- can break var's and function's definitions by lines;
- can remove trailing whitespaces and add new lines at the end of a file.

### About formatting rules

For the rule's configuration, we have several options. First, keeping default rules which mostly follows 
Clojure Style Guide except single space indentation for several forms.
It is useful for large projects which partially already have near style like that. 
Because it will be faster to add rules with minor changes of a legacy codebase.

But for my personal and new projects, I choose universal Clojure style formatting which is described 
in the article "Better Clojure formatting". I like that way to format Clojure code because it has a small number of rules, 
the same logic to treat different forms (actually, there just one exception: imports) and it is not broke if once you will add some new any type of macros.

### Configure cljstyle using universal formatting rules

To configure cljstyle please place file `.cljstyle` to a root of a project with content:

```clojure
{:indents ^:replace {#"^(require)" [[:block 0]]
                     #"^[^ \:]" [[:inner 0]]
                     #"^\w" [[:inner 0]]}
 :line-break-vars? false
 :rewrite-namespaces? false}
```

I turned off the line breaking for vars because sometimes it is convenient to have var definition in the same line with its value. 
Also, I switched off rewriting of namespaces because for my custom rules I didn't find a way to force cljstyle to do it correctly.
Most interesting part is under `:indents` key. And I will explain it line by line from the end:

- `#"^\w" [[:inner 0]]` - enable two spaces indentation for all forms starting from word character;
- `#"^(require)" [[:block 0]]` - format `require` function as block because it is most common approach to format imports;
- `#"^(-|\?|>|<|=)" [[:inner 0]]` - enable two spaces indentation for all forms starting with listed non word character. 

