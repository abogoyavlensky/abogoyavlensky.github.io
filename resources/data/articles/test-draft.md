Some interesting text.



```diff
--- a/src/some/server.clj
+++ b/src/some/server.clj
@@ -4,7 +4,7 @@


 (defrecord Server
-   [app options server]
+  [app options server]

   component/Lifecycle

Checked 8 files in 291 ms
     7 correct
     1 incorrect
Resulting diff has 2 lines
1 files formatted incorrectly
```
