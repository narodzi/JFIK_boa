
all: clean antlr compile run test

antlr:
	java -jar /usr/local/lib/antlr-4.13.1-complete.jar boa.g4

compile:
	javac -cp /usr/local/lib/antlr-4.13.1-complete.jar:. *.java

run:
	java -cp /usr/local/lib/antlr-4.13.1-complete.jar:. Main test.boa > test.ll

test:
	@echo '------------------------------'
	@lli test.ll

clean:
	rm -f *.class
	rm -f a.out
	rm -f *.ll
	rm -f *.s
	rm -f boa*.java
	rm -f *.tokens
	rm -f *.bc
	rm -f *.interp
