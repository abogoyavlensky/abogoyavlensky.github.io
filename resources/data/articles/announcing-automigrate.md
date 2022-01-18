I'm excited to announce the first release of [`automigrate`](https://github.com/abogoyavlensky/automigrate), 
the Clojure library for database auto-migration. It allows you to define models as EDN data structures 
and migrate a database schema based on a model's changes. 

To grasp the point, let's start with an example from the [examples](https://github.com/abogoyavlensky/automigrate/tree/master/examples) 
dir of the repo followed by a description of the motivation behind the tool and the chosen approach. 
If you would like to have more details, you can find the full documentation 
in the [README.md](https://github.com/abogoyavlensky/automigrate#automigrate) file of the project.


### Installation

For the following example, we will use a database running as a Docker container as described in 
[docker-compose](https://github.com/abogoyavlensky/automigrate/blob/59797c63ffd3af008dcb9825a9d8887347bf5c36/examples/docker-compose.yaml#L4-L11). 

To have `automigrate` ready for usage, please add the following alias to the project's `deps.edn` file:

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

You can choose the paths for the models file and migrations dir as you want. 
Next, please create the models file at the path you defined in the config above `resources/db/models.edn` 
with an empty map:

```clojure
{}
```

And that's it. Now you are able to run the command for making migrations. 
Because there are no models in the file, no migrations will be created:

```shell
$ clojure -X:migrations make
There are no changes in models.
```

### Usage example

A model is a representation of a database table described as an EDN structure.
Field represents a table column and looks like vector of three elements: name, type and options.

Let's add a first model:

```clojure
{:book [[:id :serial {:unique true
                      :primary-key true}]
        [:name [:varchar 256] {:null false}]
        [:description :text]]}
```

We used simple syntax for this model definition since it only has fields and doesn't yet have indexes. 
For this reason, we defined it as a vector of vectors.

After adding the first model, we can actually create our first migration:

```shell
$ clojure -X:migrations make
Created migration: resources/db/0001_auto_create_table_book.edn
Actions:
  - create table book
```

The command prints for us the relative path of the created migration and the migration actions 
that have been detected in migration. Migration can contain multiple migration actions.

Now you can check that the migration has been created but has not yet been applied:

```shell 
$ clojure -X:migrations list
[ ] 0001_auto_create_table_book.edn
```

To view the raw SQL behind the migration, we can execute the `explain` command 
with a particular migration number:

```shell
$ clojure -X:migrations explain :number 1
SQL for migration 0001_auto_create_table_book.edn:

BEGIN;
CREATE TABLE book (id SERIAL UNIQUE PRIMARY KEY, name VARCHAR(256) NOT NULL, description TEXT);
COMMIT;
```

As we can see, a transaction wraps the migration by default.

Next, we will try to actually migrate the existing migration and add the table `book` to the database.
Existing migrations will be applied one by one in order of migration number: 

```shell
$ clojure -X:migrations migrate
Migrating: 0001_auto_create_table_book...
Successfully migrated: 0001_auto_create_table_book
```

At this point, you can check the database schema. It should be changed and the table should exist.

After that, we decided to add another table `author` and foreign key on that table to the `book` table. 
For the new table, we would like to add an index by `created-at` field. So we will use map 
for the model definition with the keys `:fields` and `:indexes`:

```diff
+ {:author {:fields [[:id :serial {:unique true
+                                 :primary-key true}]
+                    [:first-name [:varchar 256]]
+                    [:second-name [:varchar 256]]
+                    [:created-at :timestamp {:default [:now]}]]
+           :indexes [[:author-created-at-idx :btree {:fields [:created-at]}]]}

   :book [[:id :serial {:unique true
                        :primary-key true}]
          [:name [:varchar 256] {:null false}]
          [:description :text]
+         [:author :integer {:null false
+                            :foreign-key :author/id
+                            :on-delete :cascade}]]}
```

```shell
$ clojure -X:migrations make
Created migration: resources/db/migrations/0002_auto_create_table_author.edn
Actions:
  - create table author
  - create index author_created_at_idx in table author
  - add column author in table book
```

In the last migration we already see multiple migration actions.
Then we will add a new field to the `book` model and change a couple of fields in the `author` model:

```diff
  {:author {:fields [[:id :serial {:unique true
                                   :primary-key true}]
                     [:first-name [:varchar 256]]
-                    [:second-name [:varchar 256]]
+                    [:second-name :text]
-                    [:created-at :timestamp {:default [:now]}]]
+                    [:created-at :timestamp {:default [:now]
+                                             :null false}]]
            :indexes [[:author-created-at-idx :btree {:fields [:created-at]}]]}
  
   :book [[:id :serial {:unique true
                        :primary-key true}]
          [:name [:varchar 256] {:null false}]
          [:description :text]
+         [:amount :integer {:null false
+                            :default 0}]
          [:author :integer {:null false
                             :foreign-key :author/id
                             :on-delete :cascade}]]}
```

```shell
$ clojure -X:migrations make
Created TEST migration: migrations/0003_auto_add_column_amount.edn
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

And finally, check the state of the migrations:

```shell
$ clojure -X:migrations list
[✓] 0001_auto_create_table_book.edn
[✓] 0002_auto_create_table_author.edn
[✓] 0003_auto_add_column_amount.edn
```

As we can see, all migrations are checked as applied.

There are more features such as migrating to a particular migration number in any direction 
and creating raw SQL migrations for specific cases. 
Detailed info about that can be found in the [documentation](https://github.com/abogoyavlensky/automigrate/tree/master#documentation) 
section of the project.


### The idea

Of course the idea is not new: generating migrations based on model changes 
defined in the project file. 
Auto-generated migrations are already implemented in Django, Ruby on Rails, Phoenix, 
and many other frameworks across different languages. In Clojure, 
the popular approach for migrating a database is to create raw SQL files by hand,  
and it is a robust and flexible way to migrate a database.
There are several great libraries that support this approach, 
such as [ragtime](https://github.com/weavejester/ragtime), [migratus](https://github.com/yogthos/migratus) 
and an external tool [flyway](https://flywaydb.org/).

I like their limitless and clear way for migration, but there are some slight downsides. 
So I would like to emphasize the main features of `automigrate` which motivated me to make the tool. 


### Motivation

The two main features of the tool are:
- the ability to view a database schema as clear EDN data without connection to a database; 
- the ability to detect a model's changes and to make migrations automatically 
without the need to write SQL, at least too often. 

And nothing more. I want to keep it as simple as it should be.

Having a full view of a database schema can be important because it allows us to understand the domain area 
of an app better. And it also reduces the need to gather pieces of a table's changes spread out over multiple migration files. 
Also, it helps us to keep the focus on the app and helps us to not switch context for making a new migration. 
As a result, for me, it feels more natural.

And to be honest, sometimes we write not-so-complex apps which don't need any special database structures, 
and for them, making migrations could become annoying routing. 
So seems that it could probably be automated, not completely, but the majority of it.    

Of course, the main downside of the auto-migration approach is less control of migrations 
and some limits for making arbitrary migrations. Also, generated SQL queries sometimes can be not well optimized.
So the goal of the project to achieve a balance between flexibility and strictness. 


### State of the project

At the moment, `automigrate` is not yet ready for production use, but I would really appreciate it 
if you would try it out for your personal projects and provide any feedback in order to make the tool better! 
Also feel free to create an issue on [GitHub](https://github.com/abogoyavlensky/abogoyavlensky.github.io/issues).  

Currently, auto-generated migrations are supported for creating, updating and deleting 
tables, columns and indexes. For now, only PostgreSQL is supported. 
Support for other databases is planned for future development.
I plan to improve and develop the project in order to make it more stable and feature-rich. 
In the project's README.md you will find 
a [roadmap draft](https://github.com/abogoyavlensky/automigrate/tree/master#roadmap-draft) section. 
I hope you will enjoy using `automigrate`!
