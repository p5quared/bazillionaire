get-coverage:
  ./mvnw verify && open ./target/jacoco-report/index.html

verify: 
  ./mvnw verify

build:
  ./mvnw package

deploy: build
  fly deploy
