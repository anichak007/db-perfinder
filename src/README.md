mkdir bin

javac -sourcepath src -d bin src/org/anichakra/tools/db/perfinder/Application.java


jar cvfe db-perfinder.jar  org.anichakra.tools.db.perfinder.Application -C bin/ .


java -Djdbc.properties=<absolute path to jdbc.properties> -Dresult.out=<absolute path to output file> -jar db-perfinder.jar
