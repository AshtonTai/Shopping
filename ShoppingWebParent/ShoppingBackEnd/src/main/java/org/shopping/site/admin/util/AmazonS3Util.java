package org.shopping.site.admin.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.S3Configuration; // 👈 ADD THIS IMPORT

public class AmazonS3Util {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Util.class);
    private static final String BUCKET_NAME;

    private static void createBucketIfNotExists() {
        S3Client client = createS3Client();
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (NoSuchBucketException e) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
            LOGGER.info("Created S3 bucket: {}", BUCKET_NAME);
        }
    }

    static {
        String bucketFromEnv = System.getenv("AWS_BUCKET_NAME");
        if (bucketFromEnv != null && !bucketFromEnv.trim().isEmpty()) {
            BUCKET_NAME = bucketFromEnv;
        } else {
            BUCKET_NAME = "shopping-app-local";
        }
    }

    /**
     * Creates an S3 client configured for LocalStack (local development).
     * Uses fake credentials and points to http://localhost:4566.
     */
    private static S3Client createS3Client() {
        AwsBasicCredentials fakeCreds = AwsBasicCredentials.create("test", "test");

        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(fakeCreds))
                .endpointOverride(URI.create("http://localhost:4566")) // LocalStack
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // 👈 CRITICAL FOR LOCALSTACK
                        .build())
                .build();
    }

    public static List<String> listFolder(String folderName) {
        S3Client s3 = createS3Client();
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(BUCKET_NAME)
                .prefix(folderName.endsWith("/") ? folderName : folderName + "/")
                .build();

        ListObjectsResponse response = s3.listObjects(listRequest);
        List<String> listKeys = new ArrayList<>();

        if (response.contents() != null) { // 👈 Safe check
            for (S3Object object : response.contents()) {
                listKeys.add(object.key());
            }
        }

        return listKeys;
    }

    public static void uploadFile(String folderName, String fileName, InputStream inputStream) {
        createBucketIfNotExists();

        S3Client client = createS3Client();

        String key = folderName.endsWith("/")
                ? folderName + fileName
                : folderName + "/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .acl("public-read")
                .build();

        try (inputStream) {
            int contentLength = inputStream.available();
            client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (IOException ex) {
            LOGGER.error("Could not upload file to S3 (LocalStack)", ex);
        }
    }

    public static void deleteFile(String fileName) {
        createBucketIfNotExists();

        S3Client client = createS3Client();

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .build();

        client.deleteObject(request);
        LOGGER.info("Deleted file: {}", fileName);
    }

    public static void removeFolder(String folderName) {
        createBucketIfNotExists();

        S3Client client = createS3Client();

        String prefix = folderName.endsWith("/") ? folderName : folderName + "/";

        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(BUCKET_NAME)
                .prefix(prefix)
                .build();

        ListObjectsResponse response = client.listObjects(listRequest);

        if (response.contents() != null) { // 👈 Safe check
            for (S3Object object : response.contents()) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(object.key())
                        .build();
                client.deleteObject(deleteRequest);
                LOGGER.info("Deleted object: {}", object.key());
            }
        }
    }
}