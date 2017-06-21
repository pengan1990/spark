./build/mvn -Pyarn -Phadoop-2.6 -Dhadoop.version=2.6.1 -Phive -Phive-thriftserver -DskipTests clean package

./dev/make-distribution.sh --tgz -Pyarn -Phadoop-2.6 -Dhadoop.version=2.6.1 -Dyarn.version=2.6.1 -Phive -Phive-thriftserver -DskipTests
