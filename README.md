# Personal blog

![CI](https://github.com/abogoyavlensky/abogoyavlensky.github.io/workflows/CI/badge.svg?branch=dev)

## Publish article

### Add new article

Being in the `dev` branch add meta info about article to `resources/data/meta.edn` 
to vector named `:articles` as map with fields:

```clojure
{:id 1  ; just to keep order
 :slug "some-article-slug"  ; will be used in url
 :title "Some article name"
 ; Date format: `YYYY-MM-DD`
 :date "2020-08-24"  ; published date
 :keywords ["tag" "will" "be" "used" "for" "seo"]
 :description "A few words about main idea of article"}
```

Then you could add new file with article itself in markdown 
to `resources/data/articles/some-article-slug.md`.

*Note that file must have the same name as article slug.*

### Deploy
Run deploy with commit message:

```shell script
make deploy 'Add some new article'
``` 

## Development

Requirements:

- java >= 11
- clj >= 1.10
- node >= 10.0.0
    - npm >= 6.0.0
- docker >= 20.10.16

Once requirements have been installed you could build css styles:

```shell script
npm install
make css-dev
```

and run for starting dev server:

```shell script
make repl
```

inside repl session please run: 

```shell script
(reset)
```

Now built and auto reloaded web app is available on: `http://localhost:8001`.  

If you would like to change tailwindcss config or add some custom css 
to `resources/public/css/styles.css` you could run building css in watch mode: 

```shell script
make css-watch
```

### Linting and formatting

To perform lint and fmt actions we use `docker-compose` for all cases: 
local checking and CI on every push to `dev` branch.

#### Linting

For first time or when some lib is added or updated you should run: 

```shell
make lint-init
```

Then on regular basis please run:

```shell
make lint
```

#### Running tests

```shell
make test
```

#### Formatting

There are two options checking code formatting:

```shell
make fmt-check
```

and fixing (*change code!*):

```shell
make fmt
```
