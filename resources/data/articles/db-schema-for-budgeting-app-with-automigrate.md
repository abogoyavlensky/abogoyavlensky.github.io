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

The dir already contains an example migrations let's remove them from migrations dir and make models.edn empty to start from scratch:

*TODO: make separated exmaple dir for this guid in the tool's repo!*

```shell
$ rm -rf migrations/*.edn && echo "{}" > models.edn
```

*Note: at the time of writing the latest version of Automigrate is `0.3.2`.*

#### Run database

After initial setup let's check that we can perform Automigrate commands. The only required local dependency is Docker. Let's install it using [official guide](https://docs.docker.com/engine/install/) if you don't have it installed. First of all let's build docker image of the `demo` service with the Docker Compose and run services:

```shell
$ docker compose build demo
$ docker compose up -d db adminer
```

*Note: `Adminer` is a handy database viewer that we can use to check actual database schema changes.*


The port `8081` should free for Adminer, we will use it to check database schema changes after applying migrations.

Let's check that we can login into Adminer and see the empty databse.
The value for username, password and database name is `demo`.

![Login to Adminer](/assets/images/articles/7_adminer_login.png)
![Empty DB state](/assets/images/articles/7_diagram_empty_db.png)

Let's also check that we can get an empty list of migrations:

```shell
$ docker compose run --rm demo clojure -X:migrations list

Migrations not found.
```

Now, we are good to reproduce all commands from this guide. And for simplifying the process let's get into shell inside the container with example project:

```shell
$ docker compose run --rm demo /bin/bash
```

*All following commands will be performed inside the container of `demo` service.*

### First model

Let's start with creating model for accounts. To make things simple in this table we can have just id, username, password and a couple of date fields to track time of changes. The model might look like:

![Empty DB state](/assets/images/articles/7_diagram_db_account.png)

To add this model open file `models.edn` and add following:

```cljoure
{:account [[:id :serial {:primary-key true}]
           [:username [:varchar 255] {:null false
                                      :unique true}]
           [:password [:varchar 255] {:null false}]
           [:updated-at :timestamp {:default [:now]}]
           [:created-at :timestamp {:default [:now]}]]}
```

Models file should contain a map where keys are model names that will be a table names in database. A value of key can be a map with keys `:fields`, `:indexes`, `:types` or a just vector if there are just fields. In this case we can simplified version and define vector of fields directly without a map.

Each element of vector of fields description caontains a vector of 3 elements: field name -> column name in database, a field type -> direct ampping to database column type (PostgreSQL in this guide) and optional map with different options of the field.

So, we added `id` and made it primary key. We decided that `username` should be varying cahracter field of length 255, unique and not null. Date fields are timestamps with current date by default at the moment of creation of a record in database.

After adding new model we can create our first migration:

```shell
$ clojure -X:migrations make
Created migration: migrations/0001_auto_create_table_account.edn
Actions:
  - create table account
```

The migration has been created automatically. The name of the migration has been generated based on first migration action from the migration. 

Now we can check list of migration and se that our migration hasn't been applied yet. This indicated an empty "box" against the migration name:

```shell
$ clojure -X:migrations list
Existing migrations:

[ ] 0001_auto_create_table_account.edn
```

The SQL for the migration looks like:

```sql
clojure -X:migrations explain :number 1
SQL for forward migration 0001_auto_create_table_account.edn:

BEGIN;
CREATE TABLE account (id SERIAL CONSTRAINT account_pkey PRIMARY KEY, username VARCHAR(255) CONSTRAINT account_username_key UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, updated_at TIMESTAMP DEFAULT NOW(), created_at TIMESTAMP DEFAULT NOW());
COMMIT;
```

We are ready to run our first migration and apply changes to the database:

```shell
$ clojure -X:migrations migrate
Applying 0001_auto_create_table_account...
0001_auto_create_table_account successfully applied.
```

Let's check list of migrations now. `x` means in the "box" that migration has been applied successfuly:

```shell
$ clojure -X:migrations list
Existing migrations:

[x] 0001_auto_create_table_account.edn
```

Let's see actual changes in database. Now we have two tables: `account` and `automigrate_migrations`. The latter is the technical table to keep statuses of applied migrations. It's created by the Automigrate and the name of the table can be chaged using configuration of the tool.

![Empty DB state](/assets/images/articles/7_db_tables_account.png)

We can see that at the moment only one migration has been applied:

![Empty DB state](/assets/images/articles/7_db_migrations_account.png)

Finnaly, we can check actual `account` table in the database:

![Empty DB state](/assets/images/articles/7_db_scheme_account.png)


### Add column


### Foreign Key and Index


### Numeric and Check constraint


### Enum and Comment


### Overview
