Exciting to announce the first release of [`automigrate`](https://github.com/abogoyavlensky/automigrate), 
the Clojure library for database auto-migration. It allows you to define models as an EDN data structure 
and migrate a database schema based on a model's changes. 

To grasp a point let's start with an example from [examples](https://github.com/abogoyavlensky/automigrate/tree/master/examples) 
dir of the repo. And then I will try to describe the motivation behind the tool and the chosen approach. 
If you would like to have more details you could find the full documentation 
at [README.md](https://github.com/abogoyavlensky/automigrate#automigrate) file of the project.


### Installation

For now for making auto-migration only PostgreSQL is supported. Other databases are in plan to future development. 
For following example we will use database running as a Docker container as it described in 
[docker-compose](https://github.com/abogoyavlensky/automigrate/blob/59797c63ffd3af008dcb9825a9d8887347bf5c36/examples/docker-compose.yaml#L4-L11). 

To have `automigrate` ready for usage you could add the following alias to the project's `deps.edn` file:

```clojure
{...
 :aliases {...
           :migrations {:extra-deps {net.clojars.abogoyavlensky/automigrate {:mvn/version "0.1.0"}
                                     org.postgresql/postgresql {:mvn/version "42.3.1"}}
                        :ns-default automigrate.core
                        :exec-args {:models-file "resources/db/models.edn"
                                    :migrations-dir "resources/db/migrations"
                                    :jdbc-url "jdbc:postgresql://localhost:5432/demo?user=demo&password=demo"}}}}
```

You can choose paths for model' file and migrations' dir as you want. 
Then create please the models' file at the path you defined in the config above `resources/db/models.edn` with empty map:

```clojure
{}
```

That's it. Now you could be able to run the command for making migrations. 
Because there are no models in the file no migrations will be created:

```shell
$ clojure -X:migrations make
There are no changes in models.
```

### Usage example

Let's add a first model:

```clojure
{:book [[:id :serial {:unique true
                               :primary-key true}]
             [:name [:varchar 256] {:null false}]
             [:description :text]]}
```

We used simple syntax for model definition which has just fields. And define it as a vector of vectors.

```shell
$ clojure -X:migrations make
Created migration: src/resources/db/0001_auto_create_table_book.edn
Actions:
  - create table book
```

Command prints for us migration actions that have been detected and defined in migrations.

Now you can check that migration's file has been created and it is not applied:

```shell 
$ clojure -X:migrations list
[ ] 0001_auto_create_table_book.edn
```

To view raw SQL behind the migration we could execute the `explain` command with a particular migration number:

```shell
$ clojure -X:migrations explain :number 1
SQL for migration 0001_auto_create_table_book.edn:

BEGIN;
CREATE TABLE book (id SERIAL UNIQUE PRIMARY KEY, name VARCHAR(256) NOT NULL, description TEXT);
COMMIT;
```

Then we will try to actually migrate existing migration and add table `book` to the database:

```shell
$ clojure -X:migrations migrate
Migrating: 0001_auto_create_table_book...
Successfully migrated: 0001_auto_create_table_book
```
At this point you can check the database schema ti should be changed and the table should exist.

After that, we decided to add another table `author` and foreign key on that table to the `book`. 
For new table we would like to an index by `created-at` field so we will use map 
for model definition with keys `:fields` and `:indexes`:

```clojure
{:author {:fields [[:id :serial {:unique true
                                              :primary-key true}]
                            [:first-name [:varchar 256]]
                            [:second-name [:varchar 256]]
                            [:created-at :timestamp {:default [:now]}]]
                :indexes [[:author-created-at-idx :btree {:fields [:created-at]}]]}

 :book [[:id :serial {:unique true
                               :primary-key true}]
             [:name [:varchar 256] {:null false}]
             [:description :text]
             [:author :integer {:null false
                                          :foreign-key :author/id
                                          :on-delete :cascade}]]}
```

```shell
$ clojure -X:migrations make
Created migration: resources/db/migrations/0002_auto_create_table_author.edn
Actions:
  - create table author
  - create index author_created_at_idx in table author
  - add column author in table book
```
Then we will add a new field to `book` model and change a couple of fields in `author` model:

```clojure
{:author {:fields [[:id :serial {:unique true
                                 :primary-key true}]
                   [:first-name [:varchar 256]]
                   [:second-name :text]
                   [:created-at :timestamp {:default [:now]
                                            :null false}]]
          :indexes [[:author-created-at-idx :btree {:fields [:created-at]}]]}

 :book [[:id :serial {:unique true
                      :primary-key true}]
        [:name [:varchar 256] {:null false}]
        [:description :text]
        [:amount :integer {:null false
                           :default 0}]
        [:author :integer {:null false
                           :foreign-key :author/id
                           :on-delete :cascade}]]}
```

```shell
$ clojure -X:migrations make
Created migration: migrations/0003_auto_add_column_amount.edn
Actions:
  - add column amount in table book
  - alter column second_name in table author
  - alter column created_at in table author
``` 

Now we would like to migrate both new migrations:

```shell
$ clojure -X:migrations migrate
Migrating: 0002_auto_create_table_author...
Successfully migrated: 0002_auto_create_table_author
Migrating: 0003_auto_add_column_amount...
Successfully migrated: 0003_auto_add_column_amount
```

And finally, check the state of migrations:

```shell
$ clojure -X:migrations list
[✓] 0001_auto_create_table_book.edn
[✓] 0002_auto_create_table_author.edn
[✓] 0003_auto_add_column_amount.edn
```

There are some more features as migrating to a particular migration number in any direction 
or creating raw SQL migrations for specific cases. 
Detailed info about that you could find in [documentation](https://github.com/abogoyavlensky/automigrate/tree/examples#documentation) 
of the project.


### The idea

The idea is not new: generating migrations based on models changes defined in the project's file as some DSL structures. 
Auto-generated migrations are already implemented in Django, Ruby on Rails, Phoenix, 
and many more frameworks in different languages. In Clojure, 
the popular approach to migrating a database is creating raw SQL files by hand. 
And it is a robust and flexible way to migrate a database. 
There are several great libraries that support the approach, 
such as [ragtime](https://github.com/weavejester/ragtime), [migratus](https://github.com/yogthos/migratus) 
and an external tool [flyway](https://flywaydb.org/).

I like that limitless and clear way for migration but there are some slight downsides. 
So I would like to emphasize the main features of the `automigrate`, which motivated me to make the tool. 


### Motivation

Two main features are:
- the ability to view database structures without connection to the database as clear EDN structure; 
- and the ability to detect model's changes and make migrations automatically without the need to touch SQL. 

Having a full view of database schema is important for me cause it allows imaging the domain area 
of an app at any time at one glance to models. It helps to understand an application's data better. 
And it reduces the need to gather pieces of table's changes spread by multiple migrations' files. 
Also, it helps to keep the focus on an app and helps to don't switch context for making a new migration. 
Eventually, it feels more natural for me. And seems that it could be automated.

Of course, the main downside of the auto-migration approach is less control of migrations 
and some limits for making migrations. Also generated queries sometimes could be not well optimized.
For that specific cases in `automigrate`, there is the ability to create [raw SQL migration](https://github.com/abogoyavlensky/automigrate#custom-sql-migration).


### State of the project

For now, `automigrate` is not ready for production use. But I would really appreciate it 
if you will try it for your personal projects. 
Any feedback would be really helpful to make the tool better! 

I plan to improve and develop the project to make it more stable and featureful. 
In the project's README.md you could find a [roadmap draft](https://github.com/abogoyavlensky/automigrate/tree/examples#roadmap-draft) section. 
Shortly, I would like to support more database column types and features, optimize auto-generated SQL queries, 
support more databases, and make reliable as possible. Hope you will enjoin using it!
