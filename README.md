# Personal blog

## Publish article

### Add new article

Add meta info about article to `resources/data/meta.edn` 
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
(user/reset)
```

Now built and auto reloaded web app is available on: `http://localhost:8000`.  

If you would like to change tailwindcss config or add some custom css 
to `resources/public/css/styles.css` you could run building css in watch mode: 

```shell script
make css-watch
```
