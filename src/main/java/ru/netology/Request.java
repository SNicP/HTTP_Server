package ru.netology;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileUploadException;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {

    public static final List<String> validPaths = List.of(
            "/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js"
    );

    private final String method;
    private final String path;
    private final InputStream body;
    private final Map<String, String> headers;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> bodyParams;
    private final Map<String, List<Part>> parts;

    public Request(String method,
                   String path,
                   InputStream body,
                   Map<String, String> headers,
                   Map<String, List<String>> queryParams,
                   Map<String, List<String>> bodyParams,
                   Map<String, List<Part>> parts) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
        this.queryParams = queryParams;
        this.bodyParams = bodyParams;
        this.parts = parts != null ? parts : Collections.emptyMap();
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public InputStream getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public Map<String, List<String>> getBodyParams() {
        return bodyParams;
    }

    public Map<String, List<Part>> getParts() {
        return Collections.unmodifiableMap(parts);
    }

    public List<Part> getPart(String name) {
        return parts.getOrDefault(name, Collections.emptyList());
    }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public Map<String, List<String>> getPostParams() {
        return Collections.unmodifiableMap(bodyParams);
    }

    public String getPostParam(String name) {
        List<String> values = bodyParams.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public String getBodyParam(String name) {
        List<String> values = bodyParams.get(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public static Request parse(BufferedInputStream inStream) throws IOException {
        final int limit = 8192;
        inStream.mark(limit);
        byte[] buffer = new byte[limit];
        int read = inStream.read(buffer);
        if (read == -1) return null;

        byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) return null;

        String requestLineStr = new String(buffer, 0, requestLineEnd, StandardCharsets.UTF_8);
        String[] requestLineParts = requestLineStr.split(" ");
        if (requestLineParts.length != 3) return null;

        String method = requestLineParts[0];
        String fullPath = requestLineParts[1];
        if (!fullPath.startsWith("/")) return null;

        byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        int headersStart = requestLineEnd + requestLineDelimiter.length;
        int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) return null;

        inStream.reset();
        long skipped = inStream.skip(headersStart);
        if (skipped != headersStart) return null;

        byte[] headersBytes = inStream.readNBytes(headersEnd - headersStart);
        String headersString = new String(headersBytes, StandardCharsets.UTF_8);
        List<String> headerLines = Arrays.asList(headersString.split("\r\n"));

        Map<String, String> headersMap = new HashMap<>();
        for (String header : List.of("Content-Length", "Host", "Connection", "Content-Type")) {
            extractHeader(headerLines, header).ifPresent(v -> headersMap.put(header, v));
        }

        skipped = inStream.skip(headersDelimiter.length);
        if (skipped != headersDelimiter.length) return null;

        int contentLength = 0;
        if (headersMap.containsKey("Content-Length")) {
            try {
                contentLength = Integer.parseInt(headersMap.get("Content-Length"));
            } catch (NumberFormatException ignored) {}
        }

        byte[] bodyBytes = new byte[0];
        InputStream bodyStream = InputStream.nullInputStream();
        if (contentLength > 0) {
            bodyBytes = inStream.readNBytes(contentLength);
            bodyStream = new ByteArrayInputStream(bodyBytes);
        }

        String path;
        Map<String, List<String>> queryParams;
        int queryStart = fullPath.indexOf('?');
        if (queryStart >= 0) {
            path = fullPath.substring(0, queryStart);
            String queryString = fullPath.substring(queryStart + 1);
            queryParams = parseQueryString(queryString);
        } else {
            path = fullPath;
            queryParams = Collections.emptyMap();
        }

        Map<String, List<String>> bodyParams = Collections.emptyMap();
        Map<String, List<Part>> parts = Collections.emptyMap();

        String contentType = headersMap.getOrDefault("Content-Type", "");

        if (contentType != null) {
            if (contentType.startsWith("multipart/form-data") && contentLength > 0) {
                parts = parseMultipart(bodyBytes, contentType);
            } else if (contentType.startsWith("application/x-www-form-urlencoded") && contentLength > 0) {
                String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
                bodyParams = parseQueryString(bodyStr);
            } else if (contentType.startsWith("text/plain") && contentLength > 0) {
                bodyParams = parsePlainTextBody(new ByteArrayInputStream(bodyBytes));
            }
        }

        return new Request(method, path, bodyStream, headersMap, queryParams, bodyParams, parts);
    }

    private static Map<String, List<String>> parsePlainTextBody(InputStream bodyStream) throws IOException {
        Map<String, List<String>> formParams = new HashMap<>();
        if (bodyStream == null) return formParams;

        String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
        String[] lines = body.split("\\r?\\n");
        for (String line : lines) {
            int eqPos = line.indexOf('=');
            if (eqPos >= 0) {
                String key = line.substring(0, eqPos);
                String value = line.substring(eqPos + 1);
                formParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
        return formParams;
    }

    private static Map<String, List<String>> parseQueryString(String query) {
        Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return Collections.emptyMap();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            String key;
            String value;
            if (index > 0) {
                key = decode(pair.substring(0, index));
                value = decode(pair.substring(index + 1));
            } else {
                key = decode(pair);
                value = "";
            }
            queryPairs.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return queryPairs;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        String prefix = header.toLowerCase() + ":";
        return headers.stream()
                .filter(h -> h.toLowerCase().startsWith(prefix))
                .map(h -> h.substring(h.indexOf(":") + 1).trim())
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i <= max - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static Map<String, List<Part>> parseMultipart(byte[] bodyBytes, String contentTypeHeader) throws IOException {
        try {
            String boundary = extractBoundary(contentTypeHeader);
            if (boundary == null) throw new IOException("Boundary not found in Content-Type header");

            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setHeaderEncoding("UTF-8");

            ByteArrayInputStream inputStream = new ByteArrayInputStream(bodyBytes);
            RequestContextImpl ctx = new RequestContextImpl(contentTypeHeader, bodyBytes.length, inputStream);
            List<FileItem> items = upload.parseRequest(ctx);

            Map<String, List<Part>> map = new LinkedHashMap<>();

            for (FileItem item : items) {
                String fieldName = item.getFieldName();
                String fileName = item.isFormField() ? null : item.getName();
                String mimeType = item.getContentType();
                byte[] content = item.get();

                Part part = new Part(fieldName, fileName, mimeType, content);
                map.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(part);
            }
            return map;

        } catch (FileUploadException e) {
            throw new IOException(e);
        }
    }

    private static String extractBoundary(String contentType) {
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.startsWith("boundary=")) {
                String boundary = param.substring("boundary=".length());
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                return boundary;
            }
        }
        return null;
    }

    private static class RequestContextImpl implements org.apache.commons.fileupload.RequestContext {
        private final String contentType;
        private final int contentLength;
        private final InputStream inputStream;

        public RequestContextImpl(String contentType, int contentLength, InputStream inputStream) {
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.inputStream = inputStream;
        }

        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public String getContentType() { return contentType; }
        @Override public int getContentLength() { return contentLength; }
        @Override public InputStream getInputStream() throws IOException { return inputStream; }
    }
}
