package dev.che1s.litesigninbedrock.update;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded synchronous HTTP client; the checker calls it exclusively off-thread. */
public final class GitHubReleaseClient {
    static final String ENDPOINT = "https://api.github.com/repos/che11111/LiteSignInBedrock/releases/latest";
    public static final String RELEASE_PAGE = "https://github.com/che11111/LiteSignInBedrock/releases/latest";
    static final int LIMIT = 512 * 1024;
    interface Connections { HttpURLConnection open() throws IOException; }
    private final Connections connections;
    public GitHubReleaseClient() { this(() -> (HttpURLConnection) new URL(ENDPOINT).openConnection()); }
    GitHubReleaseClient(Connections connections) { this.connections = connections; }

    public static final class Release {
        public final String version;
        public final String url;
        Release(String version) { this.version = version; this.url = RELEASE_PAGE; }
    }

    public Optional<Release> fetchLatest() throws IOException {
        HttpURLConnection connection = connections.open();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestProperty("User-Agent", "LiteSignInBedrock-update-checker");
            int status = connection.getResponseCode();
            if (status == 404) return Optional.empty();
            if (status != 200) throw new IOException("GitHub HTTP " + status);
            if (connection.getContentLengthLong() > LIMIT) throw new IOException("Release response too large");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    if (output.size() + length > LIMIT) throw new IOException("Release response too large");
                    output.write(buffer, 0, length);
                }
                return parse(output.toByteArray());
            }
        } finally { connection.disconnect(); }
    }

    static Optional<Release> parse(byte[] bytes) throws IOException {
        if (bytes.length > LIMIT) throw new IOException("Release response too large");
        try (JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            reader.setLenient(false);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) throw new IOException("Expected release object");
            String version = null;
            Boolean draft = null, prerelease = null;
            Set<String> seen = new HashSet<>();
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equals("tag_name") || key.equals("draft") || key.equals("prerelease")) {
                    if (!seen.add(key)) throw new IOException("Duplicate release field: " + key);
                    if (key.equals("tag_name")) {
                        if (reader.peek() != JsonToken.STRING) throw new IOException("Expected tag string");
                        version = reader.nextString();
                    } else {
                        if (reader.peek() != JsonToken.BOOLEAN) throw new IOException("Expected release boolean");
                        boolean value = reader.nextBoolean();
                        if (key.equals("draft")) draft = value; else prerelease = value;
                    }
                } else skip(reader, 0);
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT || version == null || draft == null || prerelease == null)
                throw new IOException("Incomplete release JSON");
            if (draft || prerelease) return Optional.empty();
            if (!ReleaseVersion.isStable(version)) throw new IOException("Unsupported stable release tag: " + version);
            return Optional.of(new Release(version));
        } catch (IllegalStateException error) { throw new IOException("Invalid release JSON", error); }
    }

    private static void skip(JsonReader reader, int depth) throws IOException {
        if (depth > 32) throw new IOException("Release JSON nesting limit exceeded");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                reader.beginObject();
                while (reader.hasNext()) { reader.nextName(); skip(reader, depth + 1); }
                reader.endObject(); break;
            case BEGIN_ARRAY:
                reader.beginArray();
                while (reader.hasNext()) skip(reader, depth + 1);
                reader.endArray(); break;
            default: reader.skipValue();
        }
    }
}
