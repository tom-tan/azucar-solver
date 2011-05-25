VER = v0.000
VERSION = 0.000

APP0 = azucar
BINDIR = /usr/local/bin
LIBDIR = /usr/local/lib/$(APP0)

APP = $(APP0)-$(VER)
JAR = $(APP).jar
ZIP = $(APP).zip

#WEBPAGE = http://bach.istc.kobe-u.ac.jp/sugar/
WEBTITLE = Azucar: A SAT-based Constraint Solver
PACKAGE = jp.ac.kobe_u.cs.sugar

DOCTITLE = Azucar version $(VERSION) Core API Specification
JAVADOC  = javadoc -locale en_US \
	-windowtitle '$(DOCTITLE)' -doctitle '$(DOCTITLE)' \
	-header '<em><b>$(DOCTITLE)</b></em><br>' \
	-bottom '<address><a href="$(WEBPAGE)">$(WEBTITLE)</a></address>' \
	-link http://java.sun.com/javase/6/docs/api \
	-classpath ../classes -sourcepath ../src -subpackages $(PACKAGE) \
	-public -d ../docs/api

all: build jar #javadoc zip

build:
	mkdir -p ../classes ../docs ../examples ../tools
	javac -d ../classes -cp ../src -s ../src \
	../src/jp/ac/kobe_u/cs/sugar/*.java \
	../src/jp/ac/kobe_u/cs/sugar/converter/*.java \
	../src/jp/ac/kobe_u/cs/sugar/csp/*.java \
	../src/jp/ac/kobe_u/cs/sugar/encoder/*.java \
	../src/jp/ac/kobe_u/cs/sugar/expression/*.java

zip:
	rm -f $(ZIP)
	rm -rf $(APP)
	mkdir $(APP)
	cp -pr ../src ../classes ../docs ../examples ../tools $(APP)
	mkdir $(APP)/bin
	cp -p $(APP0) Makefile manifest $(JAR) $(APP)/bin
	find $(APP) \( -name CVS -o -name .cvsignore -o -name '*~' \) -exec rm -r '{}' '+'
	zip -q -r $(ZIP) $(APP)
	rm -rf $(APP)

jar:
	jar cmf manifest $(JAR) -C ../classes .

javadoc:
	rm -rf ../docs/api
	LANG=C $(JAVADOC) $(PACKAGE)

install:
	mkdir -p $(BINDIR)
	install $(APP0) $(BINDIR)
	mkdir -p $(LIBDIR)
	cp -p $(JAR) $(LIBDIR)

clean:
	rm -rf ../docs/api ../classes
	rm -rf $(ZIP) $(JAR)

