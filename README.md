# BBCamundaBootDemo

This is one demo of Camunda (7.8) running on Spring Boot (1.5.10) and Java 8.

## Lessons learned

Here are lessons learned doing this demo - it took me considerable time to figure out, and solve "small" problems.

### Embedding Forms

When using Spring Boot and embedding forms in Camunda Modeler, Form Key shall be adapted.

From:
```
embedded:app:forms/<>.html
```
to
```
embedded:/forms/<>.html
```

### Using Connector and Spin

To use Camunda Connector and Spin, enable plugins in Engine and put this into your application/configuration Class (I wonder why is this not enabled by default ?) 

```
	@Bean
	public ConnectProcessEnginePlugin connectProcessEnginePlugin() {
	  return new ConnectProcessEnginePlugin();
	}
```

### Cammunda Connector Errors

To handle (technical ?) errors when talking with external web services set Error Code to:

```
org.camunda.connect.ConnectorException
```

### Use lambdas

To implement JavaDelegates, use lambdas - very short and practical:

```
	@Bean(name="javaDelegate")
	public JavaDelegate sayHelloDelegate() {
		
	    return execution -> System.out.println("Logging something from javaDelegate... Hello");
	}
```

### EE Camunda

To authentificate access to EE repository, there is a need to adapt (or create) ~/.m2/settings.xml with similar content

```
	<servers>
		<server>
			<id>Camunda-EE-Repository</id>
			<username>...</username>
			<password>...</password>
		</server>
	</servers>
```

### H2 Database

To se content of H2 in memory database, use following URL in web browser:
```
http://localhost:8080/h2-console
```
and following database URL (leave user sa without password):
```
jdbc:h2:mem:testdb
```
Demo table is called computers.

### SSH execution added

Try to prepare and use docker image as host with SSHD.
