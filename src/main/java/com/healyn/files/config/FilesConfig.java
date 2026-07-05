package com.healyn.files.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(HealynS3Properties.class)
public class FilesConfig {

    private static final Logger log = LoggerFactory.getLogger(FilesConfig.class);

    /** Client for server-side object operations (stat / read / delete). */
    @Bean
    @Primary
    public MinioClient minioClient(HealynS3Properties props, Environment env) {

        if (props.endpoint() == null || props.endpoint().isBlank()) {
            log.warn("HEALYN_S3_ENDPOINT is not configured or empty. Using default endpoint may fail in production.");
        }
        if (props.accessKey() == null || props.accessKey().isBlank()) {
            log.warn("HEALYN_S3_ACCESS_KEY is not configured or empty.");
        }
        if (props.secretKey() == null || props.secretKey().isBlank()) {
            log.warn("HEALYN_S3_SECRET_KEY is not configured or empty.");
        }
        if (props.publicEndpoint() == null || props.publicEndpoint().isBlank()) {
            log.info("HEALYN_S3_PUBLIC_ENDPOINT is not configured; presigned URLs will use HEALYN_S3_ENDPOINT.");
        }
        return build(props.endpoint(), props);
    }

    /**
     * Client used only to mint presigned URLs. Its endpoint host is what ends up
     * in the signed URL, so it must be a host the client (mobile device / browser)
     * can actually reach — see {@link HealynS3Properties#presignEndpoint()}.
     */
    @Bean
    public MinioClient minioPresignClient(HealynS3Properties props) {
        return build(props.presignEndpoint(), props);
    }

    private static MinioClient build(String endpoint, HealynS3Properties props) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(props.accessKey(), props.secretKey());
        if (props.region() != null && !props.region().isBlank()) {
            builder.region(props.region());
        }
        return builder.build();
    }
}
