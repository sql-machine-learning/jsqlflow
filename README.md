# SQLFlow client for Java Developers

## Requirements
 - Java 8+

## Build
### Compile protobuf
```shell script
mvn protobuf:compile && mvn protobuf:compile-custom
```
### Package
```shell script
mvn clean package -q
```

## Test
We provide two tests, one requests to local mocking server and the other requests to a remote server:
```shell script
SQLFLOW_SERVER="server address" \
SQLFLOW_SUBMITTER="pai/alisa/..." \
SQLFLOW_DATA_SOURCE="Your data source specified by https://github.com/sql-machine-learning/sqlflow/blob/develop/doc/run_with_maxcompute.md" \
USER_ID="the one who runs the SQL" \
mvn -Dtest=SQLFlowRemoteTest#testRun test
```