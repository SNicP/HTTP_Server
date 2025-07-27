package ru.netology;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

public class Part {
    private final String name;
    private final String fileName; // null если поле обычное
    private final String contentType;
    private final byte[] content;

    public Part(String name, String fileName, String contentType, byte[] content) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public boolean isFile() {
        return fileName != null;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public String toString() {
        if (isFile()) {
            return String.format("Part[file]: name=%s, filename=%s, contentType=%s, size=%d bytes",
                    name, fileName, contentType, content.length);
        } else {
            return String.format("Part[field]: name=%s, value=%s",
                    name, new String(content));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Part)) return false;
        Part part = (Part) object;
        return Objects.equals(name, part.name) &&
                Objects.equals(fileName, part.fileName) &&
                Objects.equals(contentType, part.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fileName, contentType);
    }
}
