package com.opitz.camunda.springboot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.camunda.spin.xml.SpinXmlElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 
 * @author JUR
 *
 */
@SpringBootApplication
public class BBCamundaBootDemoApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(BBCamundaBootDemoApplication.class, args);
	}
	
    /**
     * Returns bean which implements JavaDelegate interface
     * @return JavaDelegate that execute local command using Camunda context variable cliCommand and stores output/error back in context
     * 
     */
	@Bean(name="runCommandDelegate")
	public JavaDelegate runCommandDelegate() {

		return execution -> 
			{ 
				try {
					// to collect output
					StringBuffer outBuffer = new StringBuffer();

					Process proc = Runtime.getRuntime().exec( execution.getVariable("cliCommand").toString() );
					proc.getOutputStream().close();
					
					// collect output
					new BufferedReader(new InputStreamReader( proc.getInputStream()) ).lines().forEach( new Consumer<String>() {

						@Override
						public void accept(String t) {
							outBuffer.append(t).append("\n");							
						}
					});
					
					// set output as variable
					execution.setVariable("cliCommandOutput", outBuffer.toString());

					// to collect error
					StringBuffer errorBuffer = new StringBuffer();
					
					// collect error
					new BufferedReader(new InputStreamReader( proc.getErrorStream()) ).lines().forEach( new Consumer<String>() {

						@Override
						public void accept(String t) {
							errorBuffer.append(t).append("\n");							
						}
					});
					
					// set error as variable
					execution.setVariable("cliCommandError", outBuffer.toString());

				} catch(Exception ex) {
					throw new BpmnError( ex.getMessage() );
				}
			};
	}

	/**
	 * Returns bean which implements JavaDelegate interface
	 * @return JavaDelegate that logs XML node content
	 */
	@Bean(name="logServerOsDelegate")
	public JavaDelegate logServerOsDelegate() {
		
	    return execute -> 
	    	{
	    		// get Camunda variable from execution context as XML element
	    		SpinXmlElement server = (SpinXmlElement) execute.getVariable("server");
	    		
	    		// print out content of XML element
	    		System.out.println(" ** server content: os=" + server.childElement("os").textContent() + ", name=" + server.childElement("hostname").textContent());
	    		
	    	};
	}

	/**
	 * Returns bean which implements JavaDelegate interface
	 * @return dummy JavaDelegate that prints hello
	 */
	@Bean(name="javaDelegate")
	public JavaDelegate sayHelloDelegate() {
		
	    return execution -> System.out.println("Logging something from javaDelegate... Hello");
	}

	/**
	 * Enables Camunda Content Process Engine Plug-ins - needed to enable usage of Connector and SPIN plug-ins
	 * @return Camunda Content Process Engine Plug-in 
	 */
	@Bean
	public ConnectProcessEnginePlugin connectProcessEnginePlugin() {
	  return new ConnectProcessEnginePlugin();
	}
	
	/**
	 * Returns bean which implements CORS filter permissions
	 * Prevents CORS failure when using Angular front end in browser
	 * @return Cors Filter bean that allows Cross-Origin Resource Sharing (https://de.wikipedia.org/wiki/Cross-Origin_Resource_Sharing)
	 */
	@Bean
	public CorsFilter corsFilter() {
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    CorsConfiguration config = new CorsConfiguration();
	    config.setAllowCredentials(true);
	    config.addAllowedOrigin("*");
	    config.addAllowedHeader("*");
	    config.addAllowedMethod("OPTIONS");
	    config.addAllowedMethod("GET");
	    config.addAllowedMethod("POST");
	    config.addAllowedMethod("PUT");
	    config.addAllowedMethod("DELETE");
	    source.registerCorsConfiguration("/**", config);
	    return new CorsFilter(source);
	}

}
