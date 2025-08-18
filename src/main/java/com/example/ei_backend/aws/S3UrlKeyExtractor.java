package com.example.ei_backend.aws;

public class S3UrlKeyExtractor {
    private S3UrlKeyExtractor() {}

    public static String extractKey(String bucket, String region, String url) {
        if (url == null || url.isBlank()) throw new IllegalStateException("url이 없음");
        String s3Host = bucket + ".s3." + region + ".amazonaws.com";
        int schemeIdx = url.indexOf("://"); if(schemeIdx < 0) throw new IllegalStateException("invalid url: " + url);
        int hostStart = schemeIdx + 3;
        int pathStart = url.indexOf('/', hostStart);
        if (pathStart < 0) throw new IllegalStateException("no path in url:" + url);
        String host = url.substring(hostStart, pathStart);
        String path = url.substring(pathStart + 1);
        return host.equalsIgnoreCase(s3Host) ? path : path;
    }
}
