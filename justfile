get-coverage:
  ./mvnw verify && open ./target/jacoco-report/index.html

verify: 
  ./mvnw verify

build:
  ./mvnw package

deploy: build
  fly deploy

format:
  ./mvnw spotless:apply

format-check:
  ./mvnw spotless:check

native-build:
  ./mvnw package -Dnative

setup:
  git config blame.ignoreRevsFile .git-blame-ignore-revs
  git config core.hooksPath .githooks
