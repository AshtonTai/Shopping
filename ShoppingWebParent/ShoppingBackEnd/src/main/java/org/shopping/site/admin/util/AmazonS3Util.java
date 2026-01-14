package org.shopping.site.admin.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonS3Util.class);
    private static final String BUCKET_NAME;

    static {
        BUCKET_NAME = System.getenv("AWS_BUCKET_NAME");
    }

    // Centralized S3 client with region configured
    private static S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.AP_SOUTHEAST_1) // 👈 Ensure this matches your bucket's region
                .build();
    }

    public static List<String> listFolder(String folderName) {
        S3Client s3 = createS3Client();
        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(BUCKET_NAME)
                .prefix(folderName)
                .build();

        ListObjectsResponse response = s3.listObjects(listRequest);
        List<S3Object> contents = response.contents();
        List<String> listKeys = new ArrayList<>();

        for (S3Object object : contents) {
            listKeys.add(object.key());
        }

        return listKeys;
    }

    public static void uploadFile(String folderName, String fileName, InputStream inputStream) {
        S3Client client = createS3Client();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(folderName + "/" + fileName)
                .acl("public-read")
                .build();

        try (inputStream) {
            int contentLength = inputStream.available();
            client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (IOException ex) {
            LOGGER.error("Could not upload file to Amazon S3", ex);
        }
    }

    public static void deleteFile(String fileName) {
        S3Client client = createS3Client();

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .build();

        client.deleteObject(request);
    }

    public static void removeFolder(String folderName) {
        S3Client client = createS3Client();

        ListObjectsRequest listRequest = ListObjectsRequest.builder()
                .bucket(BUCKET_NAME)
                .prefix(folderName + "/")
                .build();

        ListObjectsResponse response = client.listObjects(listRequest);
        List<S3Object> contents = response.contents();

        for (S3Object object : contents) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(object.key())
                    .build();
            client.deleteObject(request);
            System.out.println("Deleted " + object.key());
        }
    }
}