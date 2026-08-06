package com.uthman.VaultApi.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

// Render injects DATABASE_URL in the format "postgres://user:pass@host:port/db",
// while Spring's JDBC driver requires "jdbc:postgresql://...". This bean
// normalizes the URL and falls back to spring.datasource.* for local/CI configs.
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${spring.datasource.url:}")
    private String springUrl;

    @Value("${spring.datasource.username:}")
    private String springUsername;

    @Value("${spring.datasource.password:}")
    private String springPassword;

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            ds.setJdbcUrl(toJdbcUrl(databaseUrl));
            if ((springUsername == null || springUsername.isBlank())
                    && (springPassword == null || springPassword.isBlank())) {
                String[] creds = parseCredentials(databaseUrl);
                if (creds != null) {
                    ds.setUsername(creds[0]);
                    ds.setPassword(creds[1]);
                }
            } else {
                ds.setUsername(springUsername);
                ds.setPassword(springPassword);
            }
        } else {
            ds.setJdbcUrl(springUrl);
            ds.setUsername(springUsername);
            ds.setPassword(springPassword);
        }

        return ds;
    }

    static String toJdbcUrl(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            int schemeEnd = url.indexOf("://");
            return "jdbc:postgresql://" + url.substring(schemeEnd + 3);
        }
        return url;
    }

    static String[] parseCredentials(String url) {
        int schemeEnd = url.indexOf("://");
        int at = url.indexOf('@', schemeEnd + 3);
        if (schemeEnd < 0 || at < 0) {
            return null;
        }
        String userInfo = url.substring(schemeEnd + 3, at);
        int colon = userInfo.indexOf(':');
        if (colon < 0) {
            return new String[]{userInfo, ""};
        }
        return new String[]{userInfo.substring(0, colon), userInfo.substring(colon + 1)};
    }
}
