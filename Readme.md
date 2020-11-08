How to Run:

- Make sure you are in root directory of project
- JDK installed in your machine and Java Path set

For Starting Server:

```shell script
# For compilation
javac -classpath src/ src/server/Server.java 
# For Execution - Arguments are mandatory
java -classpath src/ server.Server <server_id> 9001
```

For Starting Client
```shell script
# For compilation
javac -classpath src/ src/client/Client.java 
# For Execution - Arguments are mandatory
java -classpath src/ client.Client <client_id>
```

