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
	@rm -rf resources/public/css/output* dist


.PHONY: install-css-deps  # Install css deps
install-css-deps:
	@$(INFO) "Installing typography plugin..."
	@npm install


.PHONY: css-dev  # Compile css styles
css-dev:
	@$(INFO) "Node version:"
	@node -v
	@$(INFO) "Compiling css..."
	@npm run css-dev


.PHONY: css-prod  # Compile css styles for production use
css-prod:
	@$(INFO) "Node version:"
	@node -v
	@$(INFO) "Compiling css..."
	@npm run css-prod


.PHONY: watch-css  # Watching css changes and build output file with styles
watch-css:
	@$(INFO) "Watching css changes..."
	@ls tailwind.config.js resources/public/css/style.css | entr make css


.PHONY: repl  # Start repl
repl:
	@$(INFO) "Starting repl..."
	@clojure -A:dev


.PHONY: build  # Run production build
build:
	@$(INFO) "Building css..."
	@$(MAKE) css-prod
	@$(INFO) "Building html..."
	@clj -A:build
	@$(INFO) "Copying resource files to dist..."
	@mkdir -p dist/assets
	@cp -a resources/public/. dist/assets/
	@rm dist/assets/css/output.css
	@rm resources/public/css/output.*.css


.PHONY: deploy  # Deploy blog pages to production
deploy:
	@$(MAKE) clean
	@$(MAKE) build
	@$(INFO) "Checking out to master..."
	@git checkout master
	@$(INFO) "Copying resource files from dist to root..."
	@cp -a dist/. .
	@$(INFO) "Committing to master..."
	@git add *
	@git commit -am '$(GOALS)' || true
	@$(INFO) "Deploying latest blog changes..."
	@git push origin master
	@$(INFO) "Switching back to dev branch..."
	@git checkout dev
