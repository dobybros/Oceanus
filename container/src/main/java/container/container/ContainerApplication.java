package container.container;

import com.dobybros.gateway.script.GroovyServletScriptDispatcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.context.LifecycleAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.*;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import script.core.servlets.GroovyServletDispatcher;
@ComponentScan(basePackages = {"com", "container"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
		GroovyTemplateAutoConfiguration.class, AopAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class, EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
		ErrorMvcAutoConfiguration.class, GsonAutoConfiguration.class, HttpEncodingAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, JacksonAutoConfiguration.class,
		KafkaAutoConfiguration.class, LifecycleAutoConfiguration.class, MultipartAutoConfiguration.class, QuartzAutoConfiguration.class, RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class,
		RestTemplateAutoConfiguration.class, SpringDataWebAutoConfiguration.class, TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class,
		TransactionAutoConfiguration.class, WebMvcAutoConfiguration.class, WebSocketServletAutoConfiguration.class, ActiveMQAutoConfiguration.class, BatchAutoConfiguration.class, CacheAutoConfiguration.class,
		CassandraAutoConfiguration.class, CassandraReactiveDataAutoConfiguration.class, CassandraReactiveRepositoriesAutoConfiguration.class, CassandraRepositoriesAutoConfiguration.class, QuartzAutoConfiguration.class})
public class ContainerApplication {
	@Bean
	public ServletRegistrationBean servletRegistrationBean() {
		//用ServletRegistrationBean包装servlet
		ServletRegistrationBean registrationBean
				= new ServletRegistrationBean(new GroovyServletDispatcher());
		registrationBean.setLoadOnStartup(1);
		registrationBean.addUrlMappings("/rest/*");
		registrationBean.setName("groovyDispatcherServlet");
		return registrationBean;
	}
	@Bean
	public ServletRegistrationBean baseServletRegistrationBean() {
		//用ServletRegistrationBean包装servlet
		ServletRegistrationBean baseServletRegistrationBean
				= new ServletRegistrationBean(new GroovyServletScriptDispatcher());
		baseServletRegistrationBean.setLoadOnStartup(1);
		baseServletRegistrationBean.addUrlMappings("/base/*");
		baseServletRegistrationBean.setName("InternalGroovyServlet");
		return baseServletRegistrationBean;
	}
	public static void main(String[] args) {
		SpringApplication.run(ContainerApplication.class, args);
	}

}
