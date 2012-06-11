VER = v0.2.2
VERSION = 0.2.2

APP0 = azucar
BINDIR = /usr/local/bin
LIBDIR = /usr/local/lib/$(APP0)

APP = $(APP0)-$(VER)
JAR = $(APP).jar
ZIP = $(APP).zip

WEBPAGE = http://code.google.com/p/azucar-solver/
WEBTITLE = Azucar: A SAT-based Constraint Solver
PACKAGE = jp.ac.kobe_u.cs.sugar

DOCTITLE = Azucar version $(VERSION) Core API Specification
JAVADOC  = javadoc -locale en_US \
	-windowtitle '$(DOCTITLE)' -doctitle '$(DOCTITLE)' \
	-header '<em><b>$(DOCTITLE)</b></em><br>' \
	-bottom '<address><a href="$(WEBPAGE)">$(WEBTITLE)</a></address>' \
	-link http://java.sun.com/javase/6/docs/api \
	-classpath ../classes -sourcepath ../src -subpackages $(PACKAGE) \
	-public -d ../docs/api \
	-source 5

all: javac jar zip # javadoc

javac:
	javac -source 5 -Xlint:unchecked \
		-sourcepath ../src -d ../classes \
		../src/jp/ac/kobe_u/cs/sugar/*.java \
		../src/jp/ac/kobe_u/cs/sugar/*/*.java

jar:
	jar cmf manifest $(JAR) -C ../classes .

javadoc:
	rm -rf ../docs/api
	LANG=C $(JAVADOC) $(PACKAGE)

zip:
	rm -f $(ZIP)
	rm -rf $(APP)
	mkdir $(APP)
	cp -pr ../src ../classes ../docs ../examples ../tools $(APP)
	mkdir $(APP)/bin
	cp -p $(APP0) Makefile manifest $(JAR) $(APP)/bin
	find $(APP) \( -name .svn -o -name CVS -o -name .cvsignore -o -name '*~' \) -exec rm -rf '{}' '+'
	zip -q -r $(ZIP) $(APP)
	rm -rf $(APP)

install:
	mkdir -p $(BINDIR)
	install $(APP0) $(BINDIR)
	mkdir -p $(LIBDIR)
	cp -p $(JAR) $(LIBDIR)

clean:
	rm -rf ../docs/api ../classes/*
	rm -rf $(ZIP) $(JAR)

