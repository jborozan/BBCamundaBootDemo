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
