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
            String url = toJdbcUrl(databaseUrl);
            url = stripUserInfo(url);
            ds.setJdbcUrl(url);
            ds.setDriverClassName(driverFor(url));
            String[] embedded = parseCredentials(databaseUrl);
            if (springUsername != null && !springUsername.isBlank()) {
                ds.setUsername(springUsername);
                ds.setPassword(springPassword);
            } else if (embedded != null) {
                ds.setUsername(embedded[0]);
                ds.setPassword(embedded[1]);
            }
        } else {
            ds.setJdbcUrl(springUrl);
            ds.setDriverClassName(driverFor(springUrl));
            ds.setUsername(springUsername);
            ds.setPassword(springPassword);
        }

        return ds;
    }

    static String driverFor(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("jdbc:h2")) {
            return "org.h2.Driver";
        }
        if (url.contains("postgresql") || url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            return "org.postgresql.Driver";
        }
        return null;
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

    // pgjdbc treats everything before "@" as host:port, so embedded
    // credentials must be removed from the URL and supplied separately.
    static String stripUserInfo(String url) {
        int at = url.lastIndexOf('@');
        int scheme = url.indexOf("://");
        if (at < 0 || scheme < 0 || at < scheme) {
            return url;
        }
        return url.substring(0, scheme + 3) + url.substring(at + 1);
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
