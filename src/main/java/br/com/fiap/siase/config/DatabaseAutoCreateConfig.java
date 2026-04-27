package br.com.fiap.siase.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@Slf4j
@Configuration
@Profile("!test")
public class DatabaseAutoCreateConfig {

    @Value("${DB_HOST:localhost}")
    private String host;

    @Value("${DB_PORT:5432}")
    private String port;

    @Value("${DB_NAME:siase_db}")
    private String dbName;

    @Value("${DB_USER:siase_user}")
    private String user;

    @Value("${DB_PASSWORD:siase_pass}")
    private String password;

    @Primary
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        criarBancoSeNaoExistir();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setDriverClassName("org.postgresql.Driver");
        return new HikariDataSource(config);
    }

    private void criarBancoSeNaoExistir() {
        String postgresUrl = String.format("jdbc:postgresql://%s:%s/postgres", host, port);
        try (Connection conn = DriverManager.getConnection(postgresUrl, user, password)) {

            PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = ?");
            check.setString(1, dbName);

            if (!check.executeQuery().next()) {
                conn.createStatement().execute(
                        "CREATE DATABASE \"" + dbName + "\" OWNER \"" + user + "\"");
                log.info("[DatabaseAutoCreate] Banco '{}' criado com sucesso.", dbName);
            } else {
                log.info("[DatabaseAutoCreate] Banco '{}' já existe.", dbName);
            }

        } catch (Exception e) {
            log.warn("[DatabaseAutoCreate] Não foi possível verificar/criar o banco '{}': {}", dbName, e.getMessage());
        }
    }
}
