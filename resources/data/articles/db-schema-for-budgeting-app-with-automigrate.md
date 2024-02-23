Today I would like to show a step-by-step process of creating a database schema of a simple app in Clojure using [Automigrate](https://github.com/abogoyavlensky/automigrate). Automigrate is a Clojure tool to efortlessly model and change database schema using EDN-structures and auto-generated migrations. 

Auto-generated migrations can also be useful when you prototyping database schema for an app. It's a lot faster to migrate database schema based on changes to the models. The best part is that with Automigrate you can completely focus on domain logic of the app. You always know how does database schema look like wihtout being connected to a database. In case when you wnat to try some different solutions, auto-generated migrations in backward directions would be either very useful.

Currently Automigrate supports only PostgreSQL (*other databases are planned*) so in this article we will use this database. 

### A project idea

Let's say we want to create a simple personal budget tracker application. The main goal is tracking expenses/income and keeping actual balance under control. We want to be able to assign category to transactions. Also, would be greate to have multiple budgets per user with settings such as currency and a set of custom categories.

In terms of database entities we would probably need: `account` (*or users*), `budget`, `transaction` and `category`. Let's add them gradually and see how Automigrate can help us along the way.

We might end up with the database schema that looks like:

TODO: add actual image to the dir!!!!!!

![Login to Adminer](/assets/images/articles/7_db_all.png)

### Setup

#### Clone example project
To simplify the process of reproducing steps from this guide you can use the [examples](https://github.com/abogoyavlensky/automigrate/tree/master/examples) directory form the tool's repository.

```shell
$ git clone git@github.com:abogoyavlensky/automigrate.git
...

$ cd examples
```

The only required local dependency is Docker. Let's install it using [official guide](https://docs.docker.com/engine/install/) if don't have it installed.

The dir already contains an example migrations let's remove them from migrations dir and make models.edn empty to start from scratch:

```shell
$ rm -rf migrations/*.edn && echo "{}" > models.edn
```

#### Run database

After initial setup let's check that we can perform Automigrate commands. First of all let's build docker image of the `demo` service in docker compose:

```shell
$ docker compose build demo
```

Now we can run empty PostgreSQL database with a handy database viewer called `Adminer`:

```shell
$ docker compose up -d db adminer
```

The port `8081` should free for Adminer, we will use it to check database schema changes after applying migrations.

Let's check that we can login into Adminer and see the empty databse.
Username, password and database name is `demo`.

![Login to Adminer](/assets/images/articles/7_adminer_login.png)
![Empty DB state](/assets/images/articles/7_empty_db.png)

Let's also check that we can get an empty list of migrations:

```shell
$ docker compose run --rm demo clojure -X:migrations list

Migrations not found.
```

Now, we are good to reproduce all commands from this guide.

### First model


### Foreign Key and Index


### Numeric and Check constraint


### Enum and Comment


### Overview
