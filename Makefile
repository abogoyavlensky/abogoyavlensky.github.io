# Styling for output
YELLOW := "\e[1;33m"
NC := "\e[0m"
INFO := @sh -c '\
    printf $(YELLOW); \
    echo "=> $$1"; \
    printf $(NC)' VALUE


# Catch command args
GOALS = $(filter-out $@,$(MAKECMDGOALS))


# Variables
LINTING_PATHS = src dev test
TAILWINDCSS_VERSION = 1.4.6


.SILENT:  # Ignore output of make `echo` command


.PHONY: help  # Show list of targets with descriptions
help:
	@$(INFO) "Commands:"
	@grep '^.PHONY: .* #' Makefile | sed 's/\.PHONY: \(.*\) # \(.*\)/\1 > \2/' | column -tx -s ">"


.PHONY: clean  # Removes files for target, out and other temp dirs
clean:
	@$(INFO) "Cleaning project..."
	@rm -rf target resources/public/css/output.css node_modules package-lock.json


.PHONY: install-css  # Install css deps
install-css-deps:
	@$(INFO) "Installing typography plugin..."
	@npm install tailwindcss@$(TAILWINDCSS_VERSION) @tailwindcss/typography@0.1.3 > /dev/null 2>&1



.PHONY: build-css  # Compile css styles
css:
	@$(INFO) "Node version:"
	@node -v
	@$(MAKE) install-css-deps
	@$(INFO) "Compiling css..."
	@npx tailwindcss@$(TAILWINDCSS_VERSION) build resources/public/css/style.css -o resources/public/css/output.css


# TODO: update to work with clj

.PHONY: repl  # Start cljs repl
repl:
	@$(INFO) "Starting repl..."
	@clojure -A:fig:build


.PHONY: build  # Run production build
build:
	@$(INFO) "Building project..."
	@clojure -A:fig:min


.PHONY: deploy  # Deploy blog pages to production
deploy:
	@$(MAKE) clean
	@$(MAKE) build
	@$(INFO) "Copying resource files to dist..."
	@mkdir -p dist
	@cp -a resources/public/. dist/
	@mkdir -p dist/cljs-out
	@cp target/public/cljs-out/dev-main.js dist/cljs-out/dev-main.js
	@$(INFO) "Checking out to master..."
	@git checkout master
	@$(INFO) "Copying resource files from dist to root..."
	@cp -a dist/. .
	@$(INFO) "Commiting to master..."
	@git add data/* images/*
	@git commit -am '$(GOALS)'
	@$(INFO) "Deploying latest blog changes..."
	@git push origin master
	@$(INFO) "Switching back to dev branch..."
	@git checkout dev
