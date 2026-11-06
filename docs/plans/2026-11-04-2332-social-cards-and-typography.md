# Social Cards and Typography Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every page a generated 1200×630 social preview card, self-host Inter and JetBrains Mono across the site, and tidy the typography and the projects page without redesigning them.

**Tech Stack:** Clojure, Java2D (JDK, no new dependencies), Rum, Tailwind CSS 1.x with PurgeCSS, eftest

---

## Design

### Social cards

The site is static on GitHub Pages, so cards are generated during `make build`. A new namespace, `blog.og-card`, draws them with Java2D.

Output goes to `dist/assets/images/og/`:

- `<slug>.png` for each article without an override
- `site.png` for the home page, projects page, and 404

`make build` copies `resources/public/.` into `dist/assets/` with `cp -a`, which merges and does not remove the `og` folder. `make deploy` copies all of `dist/` to `master`, so the PNGs are committed there.

Card layout, all left-aligned on a white background with 80px side padding:

```
bogoyavlensky.com            JetBrains Mono Regular, 30px, #4a5568
▬▬▬▬                         4px high, 72px wide, #667eea

Deploying a full-stack       Inter Bold, 72px, #1a202c
Clojure app with Kamal       up to 3 lines
on a single server

◯ Andrey Bogoyavlenskiy      56px avatar, Inter Medium 28px, #2d3748
```

There is no date, because it makes old articles look stale when shared. The brand card uses the same template with the title `Notes mostly about programming`. Treat these sizes as starting points and tune them against real renders in Task 5.

Text fitting:

- Try title sizes `[72 60]` and use the first one that fits in 3 lines.
- If a title does not fit at 60px, throw an exception naming the slug. The fix is to shorten the title or set `:og-image`. The longest current title is 65 characters and should fit at 72px.

Java2D requirements:

- Turn on text antialiasing (grayscale, not LCD, to avoid colour fringes), fractional metrics, quality rendering, and shape antialiasing.
- Derive fonts with `TextAttribute/KERNING` set to `KERNING_ON`.
- Draw the avatar as an antialiased `Ellipse2D` filled with a `TexturePaint`. `setClip` leaves jagged edges.
- Downscale the 512px photo with area averaging first, so it does not alias at 56px.
- Set `java.awt.headless` to `true` at the top of the namespace, before any AWT use.

Fonts must be loaded inside functions (a `delay` is fine), never at the top level. Cloverage loads every namespace in `src`, and the CI image cannot initialise fonts (see Testing).

### Override

An article may set `:og-image "articles/foo_og.png"` in `meta.edn`. The path is relative to `resources/public/images/`. The build skips generating a card for that article and fails if the file is missing or is not 1200×630, because the size tags are constant.

### Meta tags

All pages are now 1200×630, so `base-og-tags` keeps constant values:

- `og:image:width` = `1200`
- `og:image:height` = `630`
- `twitter:card` = `summary_large_image`

The URL builder lives in `blog.app`:

- Articles: `https://bogoyavlensky.com/assets/images/og/<slug>.png`, or `https://bogoyavlensky.com/assets/images/<og-image>` with an override
- Other pages: `https://bogoyavlensky.com/assets/images/og/site.png`

The `?v=1` query is dropped. The URLs are new, and a query would need manual bumping.

### Web fonts

The JVM needs TTF files and the browser should get small WOFF2 files, so each font is stored in two formats.

- `resources/fonts/`: `Inter-Bold.ttf`, `Inter-Medium.ttf`, `JetBrainsMono-Regular.ttf`. These are build inputs and are not deployed.
- `resources/public/fonts/`: Latin-subset WOFF2 files from Fontsource (Inter 400, 600, 700; JetBrains Mono 400, 700), plus the OFL license texts. The licenses cover the TTF files too.

Declare the fonts with `@font-face` in `resources/public/css/style.css`, using absolute `/assets/fonts/...` URLs and `font-display: swap`. PurgeCSS keeps `@font-face` rules by default. Set `theme.extend.fontFamily` in `tailwind.config.js` so `sans` starts with `Inter` and `mono` starts with `JetBrains Mono`, followed by the current fallback stacks. Tailwind's preflight and the `font-sans` and `font-mono` utilities pick these up.

### Typography

Article width stays at `max-w-3xl`. At 20px that is already about 75–85 characters per line.

- Inline code: remove the literal backticks. Without them, inline code loses its boundary, so add a subtle background (`#edf2f7`), a small padding, a small radius, and weight 400. Reset all of this for `.prose pre code`, or code blocks get a pale background behind every block.
- Secondary text: `text-gray-600` becomes `text-gray-700` on list dates, article dates, and card descriptions.
- Article `h1`: add `font-semibold`, and swap `tracking-normal` and `leading-snug` for `tracking-tight` and `leading-tight`.
- `.prose summary` in `style.css`: use `"JetBrains Mono", Menlo, Monaco, Consolas, "Liberation Mono", monospace`.

PurgeCSS runs in `all` mode and scans `src/**/*.clj`, so every new class must appear as a complete string in the source.

### Projects page

Keep two columns and `max-w-3xl`. Header widths would disagree between pages otherwise.

- Hierarchy: section headings get `font-bold`. Project card titles become `h3`, `text-xl`, `font-semibold`. Contribution titles become `h3`, `text-lg`, `font-semibold`.
- Cards: make each card a flex column with a growing body, and pin the actions to the bottom with `mt-auto`.
- Images: `w-full h-48 object-cover object-top`, plus `border-b border-gray-200`, so previews share a height.
- Spacing: the outer grid uses `gap-16`, and the sections drop `mb-8 md:mb-16`. Today the gap is 80px + 64px.
- Buttons: `card-btn-ext` becomes a quiet outlined link with no uppercase or per-button colours: `px-3 py-1 ml-2 text-sm text-gray-800 border border-gray-300 rounded hover:bg-gray-100`, with a gray icon. Remove the `color` and `hover-color` parameters.
- Wrong contribution links: `getsentry/sentry-clj` points at let-go, and `com.github.liquidz/antq` points at sentry-clj. Fix both, and set the sentry stack to `["Clojure"]`.
- Refresh `blog_preview_home.png`, which shows an old version of the site.

### Testing

The CI image `abogoyavlensky/clojure-openjdk11-build:0.1.3` has no `libfreetype` or fontconfig (checked by listing its filesystem), so text rendering fails there.

- `wrap-lines` takes a measuring function, so wrapping is tested as pure logic everywhere.
- The render test is skipped when font rendering is unavailable. A `font-rendering-available?` helper tries to load a font and catches `Throwable`, because the failure is an `UnsatisfiedLinkError` or `NoClassDefFoundError`, not an `Exception`.
- Tests assert meta and tags as pure data, in the style of the existing tests.

Local builds run on the author's machine, and a stock JDK has working fonts there.

### Out of scope

- Removing the old photo from git history
- Running `make deploy`
- Re-scraping old links
- `resources/data/articles/clj-pulse.md`, which holds the author's own uncommitted edit: never stage or change it

## File Structure

- Create: `resources/fonts/*.ttf` - fonts for card rendering
- Create: `resources/public/fonts/*.woff2`, `resources/public/fonts/LICENSE-*.txt` - web fonts and licenses
- Create: `src/blog/og_card.clj` - wrapping, drawing, and writing cards
- Create: `test/blog/og_card_test.clj` - wrapping and render tests
- Modify: `src/blog/build.clj` - generate cards and validate overrides
- Create: `test/blog/build_test.clj` - override validation tests
- Modify: `src/blog/app.clj` - per-page image URLs
- Modify: `src/blog/pages.clj` - meta tags, typography classes, projects page
- Modify: `resources/public/css/style.css` - `@font-face`, inline code, `summary`
- Modify: `tailwind.config.js` - font families
- Modify: `resources/public/images/projects/blog_preview_home.png` - fresh screenshot
- Modify: `test/blog/app_test.clj`, `test/blog/pages_test.clj` - new expectations
- Modify: `README.md` - document `:og-image`

## Implementation Steps

### Task 1: Commit the metadata fixes

**Files:**
- Modify: `resources/data/meta.edn`
- Modify: `resources/public/images/my_photo_512x512.JPG`
- Modify: `resources/public/images/favicon_32x32.JPG`

- [ ] **Step 1: Commit only these paths**
  These changes are already in the working tree. Stage the three paths by name. Do not use `git add .`, because `clj-pulse.md` must stay out.
  Run: `git add resources/data/meta.edn resources/public/images/my_photo_512x512.JPG resources/public/images/favicon_32x32.JPG && git commit -m "Strip photo metadata and fill in empty descriptions"`
  Expected: 3 files changed, and `git status --short` still lists `clj-pulse.md`.

### Task 2: Add fonts and use them on the site

**Files:**
- Create: `resources/fonts/`, `resources/public/fonts/`
- Modify: `resources/public/css/style.css`, `tailwind.config.js`

- [ ] **Step 1: Fetch the font files**
  Work in the session scratchpad, one empty directory per download. From `https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip` take `extras/ttf/Inter-Bold.ttf`, `extras/ttf/Inter-Medium.ttf`, and `LICENSE.txt`. From `https://github.com/JetBrains/JetBrainsMono/releases/download/v2.304/JetBrainsMono-2.304.zip` take `fonts/ttf/JetBrainsMono-Regular.ttf`, and take `OFL.txt` from the same repository if the zip has none. Run `npm pack @fontsource/inter@5.3.0 @fontsource/jetbrains-mono@5.3.0` and take `files/inter-latin-{400,600,700}-normal.woff2` and `files/jetbrains-mono-latin-{400,700}-normal.woff2`. Copy these files only; do not add npm dependencies. Store the licenses as `resources/public/fonts/LICENSE-Inter.txt` and `LICENSE-JetBrainsMono.txt`.
  Run: `ls -la resources/fonts resources/public/fonts`
  Expected: 3 TTF, 5 WOFF2, and 2 license files, with each WOFF2 under about 50 KB.

- [ ] **Step 2: Add the CSS and Tailwind config**
  Add one `@font-face` per WOFF2 file at the top of `style.css`, before the `@tailwind` lines. Update `.prose summary`. Add `theme.extend.fontFamily` to `tailwind.config.js`.

- [ ] **Step 3: Check the production CSS**
  Run: `make css-prod && grep -o '@font-face' resources/public/css/output-prod.css | wc -l && grep -o 'font-family:Inter[^;}]*' resources/public/css/output-prod.css | head -3; rm -f resources/public/css/output-prod.css`
  Expected: `5`, and Inter leading the sans stack.

- [ ] **Step 4: Commit**
  `git add resources/fonts resources/public/fonts resources/public/css/style.css tailwind.config.js && git commit -m "Self-host Inter and JetBrains Mono"`

### Task 3: Apply the typography fixes

**Files:**
- Modify: `resources/public/css/style.css`, `src/blog/pages.clj`

- [ ] **Step 1: Fix inline code**
  In `style.css`, set `content: none` on `.prose code::before` and `.prose code::after`. Add the background, padding, radius, and weight from the Design. Reset background, padding, and radius for `.prose pre code`.

- [ ] **Step 2: Update the classes**
  In `pages.clj`, change the list date, article date, and card descriptions to `text-gray-700`, and update `h1-style`.

- [ ] **Step 3: Run the tests**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: 0 failures, 0 errors.

- [ ] **Step 4: Commit**
  `git add resources/public/css/style.css src/blog/pages.clj && git commit -m "Improve inline code, secondary text, and article title"`

### Task 4: Revisit the projects page

**Files:**
- Modify: `src/blog/pages.clj`

- [ ] **Step 1: Update hierarchy, layout, and spacing**
  Apply the Design's hierarchy, card, image, and spacing changes to `project-card`, `contribution-card`, `projects-section`, `contributions-section`, and `projects`.

- [ ] **Step 2: Quiet the buttons**
  Change `card-btn-ext` to `[text url]` and update its callers. Use `icons/ICON-COLOR-GRAY`. Remove `ICON-COLOR-WHITE` from `src/blog/icons.clj` if nothing else uses it.

- [ ] **Step 3: Fix the contribution links**
  `getsentry/sentry-clj` gets `https://github.com/getsentry/sentry-clj/commits?author=abogoyavlensky`. `com.github.liquidz/antq` gets `https://github.com/liquidz/antq/commits?author=abogoyavlensky`.

- [ ] **Step 4: Run the tests and commit**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: 0 failures, 0 errors.
  `git add src/blog/pages.clj src/blog/icons.clj && git commit -m "Tidy the projects page"`

### Task 5: Build the card renderer

**Files:**
- Create: `src/blog/og_card.clj`
- Test: `test/blog/og_card_test.clj`

The namespace exposes these functions, which Task 6 relies on:

```clojure
(wrap-lines text max-width measure)   ; measure: string -> width; returns a vector of lines
(render-card title)                   ; returns a 1200×630 BufferedImage
(write-card! title out-path)          ; creates parent directories and writes a PNG
```

- [ ] **Step 1: Write the wrapping test**
  Use a stub measure such as `#(* 10 (count %))`. Cover a short title (one line), a long title (several lines, none wider than the limit), and that no words are lost.

- [ ] **Step 2: Run the test (expect red)**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: failure, because `blog.og-card` does not exist.

- [ ] **Step 3: Implement `wrap-lines`, `render-card`, and `write-card!`**
  Follow the layout and Java2D requirements in the Design. Load fonts lazily. A title that does not fit throws `ex-info`.

- [ ] **Step 4: Add the render test**
  Add `font-rendering-available?` to the test file. When it returns true, assert that `render-card` returns 1200×630, that a corner pixel is white, and that some pixels are not white. Also assert that a 300-character title throws. When it returns false, skip.

- [ ] **Step 5: Run the tests (expect green)**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: 0 failures, 0 errors.

- [ ] **Step 6: Review real renders**
  From a REPL or `clojure -M -e`, write cards to the scratchpad for the shortest title, the three longest titles, and the brand text. Open them with the Read tool. Tune sizes and spacing until title, byline, and avatar look balanced and the avatar edge is smooth. If the type still looks poor after tuning, stop and tell the user, because that reopens the choice of an HTML renderer.

- [ ] **Step 7: Commit**
  `git add src/blog/og_card.clj test/blog/og_card_test.clj && git commit -m "Add social card renderer"`

### Task 6: Wire cards into the build and the pages

**Files:**
- Modify: `src/blog/app.clj`, `src/blog/pages.clj`, `src/blog/build.clj`
- Test: `test/blog/app_test.clj`, `test/blog/pages_test.clj`

- [ ] **Step 1: Update the tests**
  In `app_test.clj`, expect `og/site.png` from `base-html-meta`, `og/test-slug.png` from `article->html-meta`, and the override URL when `:og-image` is set. In `pages_test.clj`, expect `1200`, `630`, and `summary_large_image`, and use a sample `:og-image` of `https://bogoyavlensky.com/assets/images/og/site.png`.

- [ ] **Step 2: Run the tests (expect red)**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: the updated assertions fail.

- [ ] **Step 3: Implement the URLs and tags**
  Replace `OG-IMAGE` in `app.clj` with a site constant and an article URL function. Update the constants in `base-og-tags`.

- [ ] **Step 4: Run the tests (expect green)**
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: 0 failures, 0 errors.

- [ ] **Step 5: Generate cards in the build**
  Add `build-og-images` to `build.clj` and call it from `-main`. It writes `site.png` and one PNG per article without `:og-image`. Put the override check in a small function, `check-og-override!`, that reads the file and throws `ex-info` unless it exists at 1200×630.

- [ ] **Step 5a: Test the override check**
  Add `test/blog/build_test.clj` with three cases: a valid 1200×630 PNG, a missing file, and a wrong-sized PNG. Create the images in a temp directory with `BufferedImage` and `ImageIO`, which need no fonts, so these tests run in CI. `check-og-override!` must take the base directory as an argument so the tests can point it there.
  Run: `clojure -M:test --no-html --eftest-report eftest.report.pretty/report`
  Expected: 0 failures, 0 errors.

- [ ] **Step 6: Document the override**
  In the README's article metadata section, add `:og-image` as optional, with the path rule and the required size.

- [ ] **Step 7: Commit**
  `git add src/blog/app.clj src/blog/pages.clj src/blog/build.clj test/blog/app_test.clj test/blog/pages_test.clj test/blog/build_test.clj README.md && git commit -m "Use generated social cards for all pages"`

### Task 7: Verify the build and refresh the preview

**Files:**
- Modify: `resources/public/images/projects/blog_preview_home.png`

- [ ] **Step 0: Run lint and format checks**
  CI requires both. Run: `make lint && make fmt-check`
  Expected: no errors. If `clj-kondo` or `cljstyle` is not installed locally, say so in the handover rather than skipping silently.

- [ ] **Step 1: Run a clean build**
  Run: `make clean && make build`
  Expected: success. On a non-amd64 host, confirm `docker compose run marked` still works.

- [ ] **Step 2: Check the output**
  Run: `ls dist/assets/images/og | wc -l && ls dist/assets/fonts && grep -c 'summary_large_image' dist/index.html && grep -o 'og:image" content="[^"]*"' dist/blog/kubernetes-hetzner/index.html`
  Expected: 15 PNGs (14 articles plus `site.png`), the fonts listed, `1`, and the `og/kubernetes-hetzner.png` URL.

- [ ] **Step 3: Review the pages**
  Serve `dist/` with `python3 -m http.server --directory dist` and take screenshots of `/`, `/projects/`, and an article with both inline code and code blocks, at desktop and 390px widths. The snap Chromium cannot write to the temp path, so use the Playwright headless shell under `~/.cache/ms-playwright/`. Check that fonts load, code blocks have no pale background, images share a height, and buttons line up.

- [ ] **Step 4: Refresh the Blog preview**
  Take a 1200×800 screenshot of `/` and save it as `resources/public/images/projects/blog_preview_home.png`. Rebuild with `make clean && make build`.

- [ ] **Step 5: Commit**
  `git add resources/public/images/projects/blog_preview_home.png && git commit -m "Refresh blog preview screenshot"`

- [ ] **Step 6: Hand over the post-deploy steps**
  Do not deploy. Tell the user to run `make deploy`, then check a few links in the Facebook Sharing Debugger and LinkedIn Post Inspector, and re-scrape anything still showing the old image.
