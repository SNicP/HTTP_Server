package ru.netology;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    private final String method;
    private final String path;
    private final InputStream body;
    private final Map<String, String> headers;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> bodyParams;

    public static final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css",
            "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js"
    );

    public Request(String method,
                   String path,
                   InputStream body,
                   Map<String, String> headers,
                   Map<String, List<String>> queryParams,
                   Map<String, List<String>> bodyParams) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.headers = headers;
        this.queryParams = queryParams;
        this.bodyParams = bodyParams;
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
        final int limit = 4096;
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
            } catch (NumberFormatException ignored) {
            }
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
        String contentType = headersMap.getOrDefault("Content-Type", "");

        if (contentType != null && contentLength > 0) {
            if (contentType.startsWith("text/plain")) {
                bodyParams = parsePlainTextBody(new ByteArrayInputStream(bodyBytes));
            } else if (contentType.startsWith("application/x-www-form-urlencoded")) {
                String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
                bodyParams = parseQueryString(bodyStr);
            }
        }

        System.out.println("Method: " + method);
        System.out.println("Path: " + path);
        System.out.println("Headers: " + headersMap);
        System.out.println("Query params: " + queryParams);
        System.out.println("Body params: " + bodyParams);
        System.out.println("Body raw: " + (contentLength > 0 ? new String(bodyBytes, StandardCharsets.UTF_8) : "<empty>"));

        return new Request(method, path, bodyStream, headersMap, queryParams, bodyParams);
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
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
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
}
