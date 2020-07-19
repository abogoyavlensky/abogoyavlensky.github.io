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


.PHONY: clean-clj  # Removes files for target
clean-clj:
	@$(INFO) "Cleaning clj deps..."


.PHONY: clean  # Removes files for out and other temp dirs
clean:
	@$(INFO) "Cleaning project..."
	@rm -rf resources/public/css/output.css node_modules dist


.PHONY: install-css-deps  # Install css deps
install-css-deps:
	@$(INFO) "Installing typography plugin..."
	@npm install


.PHONY: css  # Compile css styles
css:
	@$(INFO) "Node version:"
	@node -v
	@$(INFO) "Compiling css..."
	@npm run build-css


.PHONY: watch-css  # Watching css changes and build output file with styles
watch-css:
	@$(INFO) "Watching css changes..."
	@ls tailwind.config.js resources/public/css/style.css | entr make css


# TODO: update to work with clj

.PHONY: repl  # Start cljs repl
repl:
	@$(INFO) "Starting repl..."
	@clojure -A:dev


.PHONY: build  # Run production build
build:
	@$(INFO) "Building project..."
#	@clojure -A:build
	@$(INFO) "Copying resource files to dist..."
	@mkdir -p dist
	@mkdir -p dist/assets
	@cp -a resources/public/. dist/assets/



#.PHONY: deploy  # Deploy blog pages to production
#deploy:
#	@$(MAKE) clean
#	@$(MAKE) build
#	@$(INFO) "Copying resource files to dist..."
#	@mkdir -p dist
#	@cp -a resources/public/. dist/
#	@mkdir -p dist/cljs-out
#	@cp target/public/cljs-out/dev-main.js dist/cljs-out/dev-main.js
#	@$(INFO) "Checking out to master..."
#	@git checkout master
#	@$(INFO) "Copying resource files from dist to root..."
#	@cp -a dist/. .
#	@$(INFO) "Commiting to master..."
#	@git add data/* images/*
#	@git commit -am '$(GOALS)'
#	@$(INFO) "Deploying latest blog changes..."
#	@git push origin master
#	@$(INFO) "Switching back to dev branch..."
#	@git checkout dev


.PHONY: deploy  # Deploy blog pages to production
deploy:
	# TODO: uncomment!
#	@$(MAKE) clean
	@$(MAKE) build
#	@cp resource/public/cljs-out/dev-main.js dist/cljs-out/dev-main.js
	@$(INFO) "Checking out to master..."
	@git checkout master
	@$(INFO) "Copying resource files from dist to root..."
	@cp -a dist/. .
	@$(INFO) "Commiting to master..."
#	@git add data/* images/*
	@git commit -am '$(GOALS)'
	@$(INFO) "Deploying latest blog changes..."
	@git push origin master
	@$(INFO) "Switching back to dev branch..."
	# TODO: update to dev branch!
	@git checkout static
