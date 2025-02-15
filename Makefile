# Styling for output
YELLOW := "\e[1;33m"
NC := "\e[0m"
INFO := @sh -c '\
    printf $(YELLOW); \
    echo "=> $$1"; \
    printf $(NC)' VALUE

DIRS?=src test

# Catch command args
GOALS = $(filter-out $@,$(MAKECMDGOALS))
SOURCE_PATHS = "src dev test .clj-kondo/hooks"

.SILENT:  # Ignore output of make `echo` command


.PHONY: help  # Show list of targets with descriptions
help:
	@$(INFO) "Commands:"
	@grep '^.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/\1 > \2/' | column -tx -s ">"


.PHONY: clean-deps  # Removes dependencies files
clean-deps:
	@$(INFO) "Cleaning project deps..."
	@rm -rf node_modules


.PHONY: clean  # Removes build files
clean:
	@$(INFO) "Cleaning project dist..."
	@rm -rf resources/public/css/output-prod.* dist


.PHONY: css-dev  # Compile css styles
css-dev:
	@$(INFO) "Node version:"
	@node -v
	@$(INFO) "Compiling css..."
	@npm run build


.PHONY: css-prod  # Compile css styles for production use
css-prod:
	@$(INFO) "Node version:"
	@node -v
	@$(INFO) "Compiling css..."
	@npm run prod


.PHONY: watch-css  # Watching css changes and build output file with styles
watch-css:
	@$(INFO) "Watching css changes..."
	@ls tailwind.config.js resources/public/css/style.css | entr make css


.PHONY: repl  # Start repl
repl:
	@$(INFO) "Starting repl..."
	@DISABLE_ANALYTICS=true clj -M:dev -r


.PHONY: test  # Run tests with coverage
test:
	@$(INFO) "Running tests..."
	@clojure -M:test


.PHONY: test-ci  # Run tests with coverage
test-ci:
	@clojure -M:test --no-html --eftest-report eftest.report.pretty/report


.PHONY: fmt-check  # Checking code formatting
fmt-check:
	@$(INFO) "Checking code formatting..."
	@cljstyle check --report $(DIRS)


.PHONY: fmt  # Fixing code formatting
fmt:
	@$(INFO) "Fixing code formatting..."
	@cljstyle fix --report $(DIRS)


.PHONY: lint  # Linting code
lint:
	@$(INFO) "Linting project..."
	@clj-kondo --config .clj-kondo/config-ci.edn --parallel --lint $(DIRS)


.PHONY: lint-init  # Linting code with libraries
lint-init:
	@$(INFO) "Linting project's classpath..."
	@clj-kondo --config .clj-kondo/config-ci.edn --parallel --lint $(shell clj -Spath)


.PHONY: marked  # Generate html from markdown
marked:
	@marked -i resources/data/articles/test-draft.md


.PHONY: build  # Run production build
build:
	@$(INFO) "Building css..."
	@$(MAKE) css-prod
	@$(INFO) "Building html..."
	@clj -M:build
	@$(INFO) "Copying resource files to dist..."
	@mkdir -p dist/assets
	@cp -a resources/public/. dist/assets/
	@cp -a resources/public/robots.txt dist/robots.txt
	@rm -f dist/assets/css/output.css
	@rm resources/public/css/output.*.css


.PHONY: deploy  # Deploy blog pages to production
deploy:
	@$(MAKE) clean
	@$(MAKE) build
	@$(INFO) "Checking out to master..."
	@git checkout master
	@$(INFO) "Copying resource files from dist to root..."
	@rm -rf blog assets
	@cp -a dist/. .
	@$(INFO) "Committing to master..."
	@git add .
	@git commit -am '$(GOALS)' || true
	@$(INFO) "Deploying latest blog changes..."
	@git push origin master
	@$(INFO) "Switching back to dev branch..."
	@git checkout dev
