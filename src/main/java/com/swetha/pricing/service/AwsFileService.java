package com.swetha.pricing.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.swetha.pricing.config.AwsS3Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class AwsFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsFileService.class);

    private AmazonS3 amazonS3;
    private AwsS3Configuration awsS3Configuration;

    @Autowired
    public AwsFileService(AmazonS3 amazonS3, AwsS3Configuration awsS3Configuration) {
        this.amazonS3 = amazonS3;
        this.awsS3Configuration = awsS3Configuration;
    }

    public void uploadFile(String text, String fileName) {
        amazonS3.putObject(awsS3Configuration.getBucketName(), fileName, text);
    }

    public String downloadFile(String fileName) throws IOException {
        S3Object s3Object = amazonS3.getObject(awsS3Configuration.getBucketName(), fileName);
        return IOUtils.toString(s3Object.getObjectContent());
    }

    public InputStream getFileAsStream(String fileName) {
        return amazonS3.getObject(awsS3Configuration.getBucketName(), fileName).getObjectContent();
    }

    public boolean fileExists(String fileName) {
        LOGGER.info("Checking if file exists, filename={}", fileName);
        return amazonS3.doesObjectExist(awsS3Configuration.getBucketName(), fileName);
    }

    public void deleteFile(String fileName) {
        amazonS3.deleteObject(awsS3Configuration.getBucketName(), fileName);
    }
}
