package org.comroid.forwardmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.comroid.api.func.util.DelegateStream;
import org.comroid.api.io.FileHandle;
import org.comroid.api.net.REST;
import org.comroid.api.os.OS;
import org.comroid.forwardmq.dto.Config;
import org.comroid.forwardmq.entity.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@ImportResource({"classpath:beans.xml"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class}, scanBasePackages = "org.comroid.forwardmq.*")
public class ForwardMQ {
	public static void main(String[] args) {
		SpringApplication.run(ForwardMQ.class, args);
	}
	@Bean(name = "configDir")
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ConditionalOnExpression(value = "environment.containsProperty('DEBUG')")
	public FileHandle configDir_Debug() {
		log.info("Using debug configuration directory");
		return new FileHandle("/srv/forwardmq-dev/", true);
	}

	@Bean
	@Order
	@ConditionalOnMissingBean(name = "configDir")
	public FileHandle configDir() {
		log.info("Using production configuration directory");
		return new FileHandle("/srv/forwardmq/", true);
	}

	@Bean
	public Config config(@Autowired ObjectMapper objectMapper, @Autowired FileHandle configDir) throws IOException {
		return objectMapper.readValue(configDir.createSubFile("config.json"), Config.class);
	}

	@Bean
	public DiscordAdapter discord(@Autowired Config config) {
		return new DiscordAdapter(config);
	}

	@Bean
	public DataSource dataSource(@Autowired Config config) {
		var db = config.getDatabase();
		return DataSourceBuilder.create()
				.driverClassName("com.mysql.jdbc.Driver")
				.url(db.getUrl())
				.username(db.getUsername())
				.password(db.getPassword())
				.build();
	}

	@Bean
	public ScheduledExecutorService scheduler() {
		return Executors.newScheduledThreadPool(32);
	}

	@Bean
	public OS.Host hostname() {
		return OS.current.getPrimaryHost();
	}

	@Bean
	@Lazy(false)
	@Transactional
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@DependsOn("applicationContextProvider")
	public Set<Entity> migrateEntities() {
		final var result = new HashSet<Entity>();
		final var helper = new Object() {
			void makeDataScheme() {
			}
		};

		return result;
	}
}
