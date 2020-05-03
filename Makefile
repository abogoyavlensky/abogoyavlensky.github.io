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


.PHONY: clean  # Removes files for target, out and other temp dirs
clean:
	@$(INFO) "Cleaning project..."
	@rm -rf target out dist


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
	@cp resources/public/index.html dist/index.html
	@$(INFO) "Checking out to master..."
	@git checkout master
	@$(INFO) "Copying resource files from dist to root..."
	@cp -a dist/. .
	@$(INFO) "Commiting to master..."
	@git commit -am '$(GOALS)'
