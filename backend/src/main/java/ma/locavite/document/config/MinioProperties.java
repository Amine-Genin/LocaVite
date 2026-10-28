package ma.locavite.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "locavite";
    private String secretKey = "locavite_dev_password";
    private String bucket = "locavite-documents";
    private String vehiclesBucket = "locavite-vehicles";
    private boolean initBucket = true;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getVehiclesBucket() {
        return vehiclesBucket;
    }

    public void setVehiclesBucket(String vehiclesBucket) {
        this.vehiclesBucket = vehiclesBucket;
    }

    public boolean isInitBucket() {
        return initBucket;
    }

    public void setInitBucket(boolean initBucket) {
        this.initBucket = initBucket;
    }
}
