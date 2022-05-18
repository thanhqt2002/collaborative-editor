countline:
	find . -name '*.java' | xargs wc -l

run:
	./gradlew run

.PHONY: countline run