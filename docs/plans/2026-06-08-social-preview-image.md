# Social Preview Image Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show Andrey's photo as the social-media preview image across the whole site, with image sizing and Twitter Card tags.

**Tech Stack:** Clojure, Rum (Hiccup), eftest

---

## Design

The site builds Open Graph meta in two places:

- `blog.app/base-html-meta` covers the main page, projects, and 404.
- `blog.app/article->html-meta` covers articles.

Both currently set `:og-image` to the generic `icon.png`. We point both at the existing photo `resources/public/images/my_photo_512x512.JPG`, which `make build` already copies into `dist/assets/images/`. The URLs are absolute, which social scrapers require.

`blog.pages/base-og-tags` renders the shared Open Graph tags and runs for both the `:website` and `:article` branches of `meta-og-tags`. Every page now shares the same 512x512 image, so we add the sizing and Twitter Card tags there once.

Attribute conventions matter: Open Graph tags use `property=`, Twitter Card tags use `name=`.

Tags to add in `base-og-tags`:

- `og:image:width` = `512` (property)
- `og:image:height` = `512` (property)
- `twitter:card` = `summary` (name) - a square card suits a 512x512 image
- `twitter:image` = `(:og-image html-meta)` (name)
- `twitter:site` = `@abogoyavlensky` (name)
- `twitter:creator` = `@abogoyavlensky` (name)

Twitter reads the title and description from the existing `og:title` and `og:description`, so we do not duplicate them.

No asset or build changes are needed. Cache-busting stays at `?v=1`; bump it if the photo is replaced later.

Testing follows the existing style in `test/blog/articles_test.clj`: call private pure functions and assert their output. We check the tag vectors from `base-og-tags` and the `:og-image` value from the app meta builders. Neither path needs Docker or marked.

## File Structure

- Modify: `src/blog/app.clj` - add an `OG-IMAGE` constant and use it in `base-html-meta` and `article->html-meta`.
- Modify: `src/blog/pages.clj` - add the sizing and Twitter Card tags in `base-og-tags`.
- Create: `test/blog/pages_test.clj` - assert `base-og-tags` emits the image, sizing, and Twitter tags.
- Create: `test/blog/app_test.clj` - assert both meta builders set `:og-image` to the photo URL.

## Implementation Steps

### Task 1: Add social meta tags to base-og-tags

**Files:**
- Modify: `src/blog/pages.clj`
- Test: `test/blog/pages_test.clj`

- [ ] **Step 1: Write the focused test**
  In `test/blog/pages_test.clj`, call `(#'blog.pages/base-og-tags sample)`, where `sample` holds `:title`, `:description`, `:canonical`, `:og-type :website`, and `:og-image "https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"`. Assert the returned vector contains the six new meta vectors: `og:image:width` and `og:image:height` with `:property`, and `twitter:card`, `twitter:image`, `twitter:site`, `twitter:creator` with `:name`. Also assert the existing `og:image` is still present.

- [ ] **Step 2: Run the test (expect red)**
  Run: `clj -M:test`
  Expected: the new pages test fails because the tags are missing.

- [ ] **Step 3: Implement the change**
  In `base-og-tags`, append the six meta vectors from the Design. Use `:property` for the `og:*` tags and `:name` for the `twitter:*` tags. Set `twitter:image` to `(:og-image html-meta)`.

- [ ] **Step 4: Run the test (expect green)**
  Run: `clj -M:test`
  Expected: all tests pass.

### Task 2: Point og:image at the photo

**Files:**
- Modify: `src/blog/app.clj`
- Test: `test/blog/app_test.clj`

- [ ] **Step 1: Write the focused test**
  In `test/blog/app_test.clj`, assert `(:og-image (#'blog.app/base-html-meta "Blog" nil))` equals `"https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"`. Assert the same `:og-image` value for `(#'blog.app/article->html-meta sample-article)`, where `sample-article` has at least `:title`, `:description`, `:keywords`, `:slug`, and `:date`.

- [ ] **Step 2: Run the test (expect red)**
  Run: `clj -M:test`
  Expected: the new app test fails because the value still points to `icon.png`.

- [ ] **Step 3: Implement the change**
  Add a private constant `OG-IMAGE` set to `"https://bogoyavlensky.com/assets/images/my_photo_512x512.JPG?v=1"`. Replace the `:og-image` expressions in `base-html-meta` and `article->html-meta` with `OG-IMAGE`. This also removes the current trailing-slash inconsistency between the two builders.

- [ ] **Step 4: Run the test (expect green)**
  Run: `clj -M:test`
  Expected: all tests pass.

### Task 3: End-to-end verification

**Files:** none

- [ ] **Step 1: Render the main page and check the tags**
  Run: `clj -M -e "(require 'blog.app) (print (blog.app/index nil nil))" | grep -E "my_photo_512x512|twitter:card|og:image:width"`
  Expected: lines for the photo URL, `twitter:card`, and `og:image:width` all appear.

- [ ] **Step 2: Confirm the photo ships in the build (optional, needs npm and docker)**
  Run: `make build && ls dist/assets/images/my_photo_512x512.JPG && grep -l my_photo_512x512 dist/index.html`
  Expected: the file exists under `dist/assets/images/`, and the URL appears in `dist/index.html`.

- [ ] **Step 3: Validate live after deploy**
  After deploying, check the home page and one article with the Twitter Card Validator, the Facebook Sharing Debugger, and the LinkedIn Post Inspector. Expected: each shows the photo. Re-scrape if a platform cached the old icon.
