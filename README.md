# Personal blog

## Publish article

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
