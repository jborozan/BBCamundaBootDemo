package com.opitz.camunda.springboot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.camunda.spin.xml.SpinXmlElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * 
 * @author JUR
 *
 */
@SpringBootApplication
public class BBCamundaBootDemoApplication {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
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

	/**
	 * Returns a bean that inits H2 database in memory and creates table computers
	 * @return
	 */
	@Bean
	public DataSource dataSource() {

		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder
			.setType(EmbeddedDatabaseType.H2)
			.addScript("db/schema.sql")
			.build();
		return db;
	}
	
	/**
	 * Returns java delegate that saves sub elements from XML server element into database
	 * @return java delegate
	 */
	@Bean(name="dbServerOsDelegate")
	public JavaDelegate dbServerOsDelegate() {
		
	    return execute -> 
	    	{	    		
	    		// get Camunda variable from execution context as XML element
	    		SpinXmlElement server = (SpinXmlElement) execute.getVariable("server");
	    		
	    		// create and extract data
	    		int id = (new Random().nextInt(1000) + 1);
	    		String os = server.childElement("os").textContent();
	    		String hostname = server.childElement("hostname").textContent();
	    		
	    		// save them
	    		jdbcTemplate.update("insert into computers(id, hostname, os) values(?, ?, ?)",
	    				new Object[] { id, hostname, os});
	    		
	    		// print out saved data too
	    		System.out.println(" ** server content saved to db: id=" + id + ", os=" + os + ", hostname=" + hostname );
	    		
	    	};
	}


	@Bean(name="sshCommandDelegate")
	public JavaDelegate sshCommandDelegate() {
		
	    return execution -> 
	    	{	    		
				try {
					// collect form data
					String sshUser = execution.getVariable("sshUser").toString();
					String sshHost = execution.getVariable("sshHost").toString();
					int sshPort = Integer.valueOf(execution.getVariable("sshPort").toString());
					String sshCommand = execution.getVariable("sshCommand").toString();					
					String sshPass = execution.getVariable("sshPass").toString();
					String sshPrivKeyFile = execution.getVariable("sshPrivKeyFile").toString();
					String sshKnownHosts = execution.getVariable("sshKnownHosts").toString();

					// print out for control check
		    		System.out.println(" ** ssh form data: user=" + sshUser + ", host=" + sshHost + ", port=" + sshPort + ", command=" + sshCommand + ", pass=" + sshPass + ", privKeyFile=" + sshPrivKeyFile + ", sshKnownHosts=" + sshKnownHosts);

		    		// start creating ssh
					JSch jsch=new JSch();
					
					// very important actually
					if( !sshKnownHosts.isEmpty() )
					{
						jsch.setKnownHosts(sshKnownHosts);
					}
					
					// if private key file is entered then we might have pass as passphase
					if(!sshPrivKeyFile.isEmpty() &&  !sshPass.isEmpty())
					{
						jsch.addIdentity(sshPrivKeyFile, sshPass);
					}
					// if private key file is entered and no passphase
					else if (!sshPrivKeyFile.isEmpty() &&  sshPass.isEmpty())
					{
						jsch.addIdentity(sshPrivKeyFile);
					}

					Session session = jsch.getSession(sshUser, sshHost, sshPort);

					// if no private key file is entered then we have password
					if(!sshPass.isEmpty() && sshPrivKeyFile.isEmpty())
					{
						session.setPassword(sshPass);
					}
					
					// prepare session
					session.connect();
					
					// prepare execution channel
					ChannelExec channel = (ChannelExec) session.openChannel("exec");
					channel.setCommand(sshCommand);

					// prepare out & err
					BufferedReader input = new BufferedReader(new InputStreamReader( channel.getInputStream()) );
					BufferedReader error = new BufferedReader(new InputStreamReader( channel.getErrStream()) );
					
					// do the command
					channel.connect();

					// to collect output
					StringBuffer outBuffer = new StringBuffer();

					// to collect error
					StringBuffer errorBuffer = new StringBuffer();

					// so collect output
					input.lines().forEach( new Consumer<String>() {

						@Override
						public void accept(String t) {
							outBuffer.append(t).append("\n");							
						}
					});
										
					// and try to collect error
					if(channel.isConnected())
					{
						// collect error
						error.lines().forEach( new Consumer<String>() {
	
							@Override
							public void accept(String t) {
								errorBuffer.append(t).append("\n");							
							}
						});
					}

					// close the things
				    channel.disconnect();
				    session.disconnect();				    

					// set output as variable
					execution.setVariable("sshCommandOutput", outBuffer.toString());

		    		// System.out.println(" ** ssh execution: out=" + outBuffer.toString());

					// set error as variable
					execution.setVariable("sshCommandError", errorBuffer.toString());

		    		// System.err.println(" ** ssh execution: err=" + errorBuffer.toString());

				} catch(Exception ex) {
					// System.err.println( ex.getMessage() );
					throw new BpmnError( ex.getMessage() );
				}
			};
	}

}
