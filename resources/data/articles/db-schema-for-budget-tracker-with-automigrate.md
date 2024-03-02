Today I would like to show a step-by-step process of creating a database schema of a simple app in Clojure using [Automigrate](https://github.com/abogoyavlensky/automigrate). Automigrate is a Clojure tool that allows efortlessly model and change database schema using EDN-structures and auto-generated migrations. 

Auto-generated migrations can also be useful when you prototyping database schema for an app. It's a lot faster to migrate database schema based on changes to the models. The best part is that with Automigrate you can completely focus on domain logic of the app. You always know how does database schema look like wihtout even being connected to a database. In case when you wnat to try some different solutions, auto-generated migrations in backward directions would be either very useful to quickly revert some experimental changes and apply different onces.

Currently Automigrate supports only PostgreSQL (*other databases are planned*) so in this article we will use this database. 

### A project idea

Let's say we want to create a simple personal budget tracker application. The main goal is tracking expenses/income and keeping actual balance under control. We want to be able to assign category to transactions. Also, would be greate to have multiple budgets per user with settings such as currency and a set of custom categories.

In terms of database entities we would probably need: `account` (*or users*), `budget`, `transaction` and `category`. Let's add them gradually and see how Automigrate can help us along the way.

We might end up with the database schema that looks like:

TODO: add actual image to the dir!!!!!!

![Login to Adminer](/assets/images/articles/7_db_all.png)

### Setup

#### Clone example project
To simplify the process of reproducing steps from this guide you can use the [example setup](https://github.com/abogoyavlensky/automigrate/tree/master/examples/empty) directory from the tool's repository.

```shell
$ git clone git@github.com:abogoyavlensky/automigrate.git
...

$ cd examples/empty
```

The dir already contains minimal setup and empty `models.edn` file. All you need locally is Docker that you can install using [official guide](https://docs.docker.com/engine/install/).


*Note: at the time of writing the latest version of Automigrate is `0.3.2`.*

#### Run database

After initial setup let's check that we can perform Automigrate commands. First of all let's build docker image of the `demo` service with the Docker Compose and run database service:

```shell
$ docker compose build demo
$ docker compose up -d db
```

Let's check that we can get an empty list of migrations:

```shell
$ docker compose run --rm demo clojure -X:migrations list
Migrations not found.
```

Now, we are good to reproduce all commands from this guide. For convenience let's get into shell inside the container with example project:

```shell
$ docker compose run --rm demo /bin/bash
```

*All following commands will be executed inside the container of `demo` service.*

#### Database viewer (optional)

`Adminer` is a handy database viewer that we can use to check actual database schema changes:

```shell
$ docker compose up -d adminer
```

The port `8081` should be free for Adminer. Let's check that we can login into Adminer and see the empty databse state.

*The value for username, password and database name is `demo`.*

![Login to Adminer](/assets/images/articles/7_adminer_login.png)
![Empty DB state](/assets/images/articles/7_diagram_empty_db.png)

### First model

Let's start with creating model for accounts. To make things simple in this table we can have just id, username, password and a couple of date fields to track time of changes. The model might look like:

![DB diagram account](/assets/images/articles/7_diagram_db_account.png)

#### Add model

To add this model open file `models.edn` and add following:

```clojure
{:account [[:id :serial {:primary-key true}]
           [:username [:varchar 255] {:null false
                                      :unique true}]
           [:password [:varchar 255] {:null false}]
           [:updated-at :timestamp {:default [:now]}]
           [:created-at :timestamp {:default [:now]}]]}
```

Models file should contain a map where keys are model names that will be a table names in database. A value of key can be a map with keys `:fields`, `:indexes`, `:types` or a just vector if there are just fields. In this case we can simplified version and define vector of fields directly without a map.

A field description is a vector of 3 elements: field name -> column name in database, field type -> [direct mapping](https://github.com/abogoyavlensky/automigrate/tree/master?tab=readme-ov-file#fields) to database column type (PostgreSQL in this guide) and optional map with different options of the field.

So, we added `id` and made it primary key. We decided that `username` should be varying cahracter field of length 255, unique and not null. Date fields are timestamps with current date by default at the moment of creation of a record in database.

#### Make migration

After adding new model we can create our first migration:

```shell
$ clojure -X:migrations make
Created migration: migrations/0001_auto_create_table_account.edn
Actions:
  - create table account
```

The migration has been created automatically. The name of the migration has been generated based on first migration action from the migration. 

#### List existing migrations

Now we can check list of migration and see that our migration hasn't been applied yet. This is indicated by an empty "box" against the migration name:

```shell
$ clojure -X:migrations list
Existing migrations:
[ ] 0001_auto_create_table_account.edn
```

We can also check the SQL for the migration:

```sql
clojure -X:migrations explain :number 1
SQL for forward migration 0001_auto_create_table_account.edn:

BEGIN;

CREATE TABLE account (
  id SERIAL CONSTRAINT account_pkey PRIMARY KEY,
  username VARCHAR(255) CONSTRAINT account_username_key UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  updated_at TIMESTAMP DEFAULT NOW(),
  created_at TIMESTAMP DEFAULT NOW()
);

COMMIT;
```

#### Apply migration to databse

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

#### Check changes in database

Let's see actual changes in database. Now we have two tables: `account` and `automigrate_migrations`. The latter is the technical table to keep statuses of applied migrations. It's created by the Automigrate and the name of the table can be chaged using configuration of the tool.

![DB tables account](/assets/images/articles/7_db_tables_account.png)

We can see that at the moment only one migration has been applied:

![Db migrations account](/assets/images/articles/7_db_migrations_account.png)

Finnaly, we can check `account` table in the database:

![DB scheme account](/assets/images/articles/7_db_scheme_account.png)


### Add column

At some point we relised that would be great also to know an email of the account. We can just add this new field to the model:

```clojure
 {:account [[:id :serial {:primary-key true}]
            [:username [:varchar 255] {:null false
                                       :unique true}]
            [:password [:varchar 255] {:null false}]
+           [:email [:varchar 255]]
            [:updated-at :timestamp {:default [:now]}]
            [:created-at :timestamp {:default [:now]}]]}
```

Then run `make` and `migrate`:

```shell
$ clojure -X:migrations make
Created migration: migrations/0002_auto_add_column_email_to_account.edn
Actions:
  - add column email to account
$ clojure -X:migrations migrate
Applying 0002_auto_add_column_email_to_account...
0002_auto_add_column_email_to_account successfully applied.
```

The new column `email` is created in the database:

![DB scheme account with email](/assets/images/articles/7_db_scheme_account_email.png)

### Foreign Key and Index

To store different budgets and settings for them we can create a budget table. The diagram will look like:

![DB diagram budget](/assets/images/articles/7_db_diagram_budget.png)

One user can have multiple budgets, so we need Foreign Key on `account` table by id. Also, would be good to have a unique index by account and title, because it is possible that different users might name their budgets the same, but for one user budget name should be unique

The changes to the models:

```clojure
 {:account [[:id :serial {:primary-key true}]
            [:username [:varchar 255] {:null false
                                       :unique true}]
            [:password [:varchar 255] {:null false}]
            [:email [:varchar 255]]
            [:updated-at :timestamp {:default [:now]}]
            [:created-at :timestamp {:default [:now]}]]
 
+ :budget {:fields [[:id :serial {:primary-key true}]
+                   [:owner-id :integer {:foreign-key :account/id
+                                        :on-delete :cascade
+                                        :null false}]
+                   [:title [:varchar 255] {:null false}]
+                   [:currency [:varchar 3] {:null false}]
+                   [:updated-at :timestamp {:default [:now]}]
+                   [:created-at :timestamp {:default [:now]}]]
+          :indexes [[:budget-owner-title-unique-idx
+                     :btree
+                     {:fields [:owner-id :title]
+                      :unique true}]]}}
```

The structure of index description is exactly the same as for a field, but options in the third argument are required and contains index-specific things.

To make a migration and apply it to database we run:

```shell
$ clojure -X:migrations make
Created migration: migrations/0003_auto_create_table_budget_etc.edn
Actions:
  - create table budget
  - create index budget_owner_title_unique_idx on budget
$ clojure -X:migrations migrate
Applying 0003_auto_create_table_budget_etc...
0003_auto_create_table_budget_etc successfully applied.
```

New table `budget` is created in the database. We can see that the index and Foreign Key is added as we described.

![DB scheme budget](/assets/images/articles/7_db_scheme_budget.png)

### Check, Enum, Comment

Finaly we are going to add tables for categories and transactions. would be good to have separated sets of categories for different budets, so `category` will have Foreign Key on `bidget`. Transaction amount can be positive and negative numberic field. So, categories either should be splitted between spending and incoming types. So the result database schema will look like:

![DB scheme budget](/assets/images/articles/7_db_diagram_full.png)

To implement this schema we can add following changes to our `models.edn`:

```clojure
 {...
+ :category {:fields [[:id :serial {:primary-key true}]
+                     [:budget-id :integer {:foreign-key :budget/id
+                                           :on-delete :cascade
+                                           :null false}]
+                     [:title [:varchar 255] {:null false}]
+                     [:icon [:varchar 255]]
+                     [:tx-type
+                      [:enum :tx-type-enum]
+                      {:default "spending"
+                       :null false
+                       :comment "Transaction direction"}]
+                     [:updated-at :timestamp {:default [:now]}]
+                     [:created-at :timestamp {:default [:now]}]]
+            :types [[:tx-type-enum :enum {:choices ["spending" "income"]}]]
+            :indexes [[:category-account-title-tx-type-unique-idx
+                       :btree
+                       {:fields [:budget-id :title :tx-type]
+                        :unique true}]]}
+
+ :transaction [[:id :serial {:primary-key true}]
+               [:budget-id :integer {:foreign-key :budget/id
+                                     :on-delete :cascade
+                                     :null false}]
+               [:category-id :integer {:foreign-key :category/id
+                                       :on-delete :cascade
+                                       :null false}]
+               [:amount [:numeric 12 2] {:null false
+                                         :check [:<> :amount 0]}]
+               [:note [:varchar 255]]
+               [:updated-at :timestamp {:default [:now]}]
+               [:created-at :timestamp {:default [:now]}]]}
```

Transaction amount can be positive or negative, but can't be 0. We added this validaton using Check Constraint `[:<> :amount 0]`. Check constraint can be presented in HoneySQL syntax.

For category we should define transaction type and we used custom Enum type with possible values: `spending`, `income`. The structure of custom type definition is the same as for the field with required options. So we need to add Enum type defintion if `:types` key of the model and then we can use it as a value for `:tx-type` field definition.

To clarify the meaning of the `:tx-type` field of `category` model we added a comment to the field. This comment will be displayed in the database as well.

Let's make migration and apply it:

```shell
$ clojure -X:migrations make
Created migration: migrations/0004_auto_create_type_tx_type_enum_etc.edn
Actions:
  - create type tx_type_enum
  - create table category
  - create table transaction
  - create index category_account_title_tx_type_unique_idx on category
$ clojure -X:migrations migrate
Applying 0004_auto_create_type_tx_type_enum_etc...
0004_auto_create_type_tx_type_enum_etc successfully applied.
```

Then we can check that database has been updated according our changes to the models.

![DB scheme budget](/assets/images/articles/7_db_scheme_category.png)

![DB scheme budget](/assets/images/articles/7_db_scheme_transaction.png)

### Overview

We've seen how we can model and change database schema in Clojure application using Automigrate. With the library you can focus on domain part of the app and do not switch context on SQL. You always see the schema of the database in models and you don't need to gather all changes across multiple SQL-migrations. That's how I see the main benefits of the tool and what I wanted to show in this article. Thank you for the attention, and I hope it was useful.
