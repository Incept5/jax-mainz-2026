// Java port of analyse_sentiment_kaggle_ollama.py.
//
// Single-file program — no Maven, no dependencies. Run with:
//
//   java AnalyseSentimentKaggleOllama.java
//
// Requires Java 11+ (uses java.net.http.HttpClient and source-file launching).
//
// On first run, downloads the Trump-tweets dataset from Kaggle into
// ~/.cache/kagglehub/ (same location kagglehub uses, so the Python and Java
// versions share a cache). Needs Kaggle credentials in ~/.kaggle/kaggle.json
// or in KAGGLE_USERNAME / KAGGLE_KEY env vars — get a token from
// https://www.kaggle.com/settings/account.
//
// Ollama must be running locally with the qwen3.5:4b model pulled.

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AnalyseSentimentKaggleOllama {

    static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    static final String MODEL = "qwen3.5:4b";
    static final int SAMPLE_SIZE = 20;

    static final String KAGGLE_DATASET = "austinreese/trump-tweets";

    static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        Path csv = findKaggleCsv();
        if (csv == null) {
            System.out.println("Kaggle CSV not found in cache; downloading...");
            csv = downloadKaggleCsv();
        }
        System.out.println("Reading CSV: " + csv);

        List<String> tweets = loadTweets(csv);
        System.out.printf("Loaded %,d tweets.%n", tweets.size());

        Collections.shuffle(tweets, new Random(42));
        List<String> sample = tweets.subList(0, Math.min(SAMPLE_SIZE, tweets.size()));
        System.out.printf("%nClassifying %d tweets via Ollama (%s)...%n%n", sample.size(), MODEL);

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("positive", 0);
        counts.put("neutral", 0);
        counts.put("negative", 0);

        for (String tweet : sample) {
            String sentiment = analyseSentiment(tweet);
            counts.merge(sentiment, 1, Integer::sum);
            String preview = tweet.replace("\n", " ");
            if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
            System.out.printf("  [%8s]  %s%n", sentiment.toUpperCase(), preview);
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("\nSummary:");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            double pct = total > 0 ? 100.0 * e.getValue() / total : 0;
            System.out.printf("  %8s: %3d  (%5.1f%%)%n", e.getKey(), e.getValue(), pct);
        }
    }

    static String analyseSentiment(String text) throws Exception {
        String prompt = "Analyse the sentiment of the following text and respond with exactly one word: "
                + "'positive', 'neutral', or 'negative'.\nText: " + text + "\nSentiment:";

        String body = "{"
                + "\"model\":" + jsonString(MODEL) + ","
                + "\"prompt\":" + jsonString(prompt) + ","
                + "\"stream\":false,"
                + "\"think\":false"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        String s = extractJsonField(resp.body(), "response").toLowerCase();
        if (s.contains("positive")) return "positive";
        if (s.contains("negative")) return "negative";
        return "neutral";
    }

    // Find the cached trumptweets.csv under ~/.cache/kagglehub/, regardless of version dir.
    static Path findKaggleCsv() throws Exception {
        Path root = Paths.get(System.getProperty("user.home"), ".cache", "kagglehub");
        if (!Files.isDirectory(root)) return null;
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().equals("trumptweets.csv"))
                    .findFirst()
                    .orElse(null);
        }
    }

    // Mirror what kagglehub does: hit the Kaggle dataset-download API with basic
    // auth, follow the redirect to the CDN, save the zip into the same cache
    // tree kagglehub uses, then extract it.
    static Path downloadKaggleCsv() throws Exception {
        String[] creds = readKaggleCredentials();
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((creds[0] + ":" + creds[1]).getBytes());

        Path dest = Paths.get(System.getProperty("user.home"), ".cache", "kagglehub",
                "datasets", "austinreese", "trump-tweets", "versions", "1");
        Files.createDirectories(dest);
        Path zip = dest.resolve("dataset.zip");

        URI url = URI.create("https://www.kaggle.com/api/v1/datasets/download/" + KAGGLE_DATASET);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest req = HttpRequest.newBuilder(url)
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<Path> resp = client.send(req, HttpResponse.BodyHandlers.ofFile(zip));
        if (resp.statusCode() != 200) {
            Files.deleteIfExists(zip);
            throw new RuntimeException("Kaggle download failed: HTTP " + resp.statusCode()
                    + " (check credentials and dataset slug '" + KAGGLE_DATASET + "')");
        }

        System.out.println("Extracting archive...");
        Path csv = null;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest)) continue; // zip-slip guard
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                if (out.getFileName().toString().equals("trumptweets.csv")) csv = out;
            }
        }
        Files.deleteIfExists(zip);
        if (csv == null) throw new RuntimeException("trumptweets.csv not found in archive");
        return csv;
    }

    // Prefer KAGGLE_USERNAME / KAGGLE_KEY env vars, fall back to ~/.kaggle/kaggle.json.
    static String[] readKaggleCredentials() throws Exception {
        String username = System.getenv("KAGGLE_USERNAME");
        String key = System.getenv("KAGGLE_KEY");
        if (username == null || key == null) {
            Path file = Paths.get(System.getProperty("user.home"), ".kaggle", "kaggle.json");
            if (!Files.exists(file)) {
                throw new RuntimeException("Kaggle credentials not found. Set KAGGLE_USERNAME and "
                        + "KAGGLE_KEY env vars, or save a token at " + file
                        + " (get one at https://www.kaggle.com/settings/account).");
            }
            String json = Files.readString(file);
            if (username == null) username = extractJsonField(json, "username");
            if (key == null) key = extractJsonField(json, "key");
        }
        if (username == null || username.isEmpty() || key == null || key.isEmpty()) {
            throw new RuntimeException("Kaggle credentials are missing username or key.");
        }
        return new String[] {username, key};
    }

    // Reads the "content" column from each CSV row. Handles quoted fields and
    // multi-line records (quotes spanning newlines).
    static List<String> loadTweets(Path csv) throws Exception {
        List<String> rows = new ArrayList<>();
        String all = Files.readString(csv);

        List<String[]> records = parseCsv(all);
        if (records.isEmpty()) return rows;

        String[] header = records.get(0);
        int idx = -1;
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim();
            if (h.equals("content") || h.equals("text") || h.equals("tweet")) { idx = i; break; }
        }
        if (idx < 0) throw new RuntimeException("No content/text/tweet column in CSV header");

        for (int r = 1; r < records.size(); r++) {
            String[] fields = records.get(r);
            if (fields.length > idx) {
                String t = fields[idx];
                if (!t.isBlank()) rows.add(t);
            }
        }
        return rows;
    }

    // Minimal RFC-4180-ish CSV parser: handles quoted fields, escaped quotes (""), and quoted newlines.
    static List<String[]> parseCsv(String text) {
        List<String[]> out = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                    row.add(field.toString());
                    field.setLength(0);
                    out.add(row.toArray(new String[0]));
                    row = new ArrayList<>();
                } else {
                    field.append(c);
                }
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            out.add(row.toArray(new String[0]));
        }
        return out;
    }

    // Pull the value of a top-level string field from a small JSON object.
    // Cheap and cheerful — fine for a teaching demo, not for production parsing.
    static String extractJsonField(String json, String name) {
        String key = "\"" + name + "\"";
        int i = json.indexOf(key);
        if (i < 0) return "";
        i = json.indexOf(':', i + key.length());
        if (i < 0) return "";
        i = json.indexOf('"', i + 1);
        if (i < 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int j = i + 1; j < json.length(); j++) {
            char c = json.charAt(j);
            if (c == '\\' && j + 1 < json.length()) {
                char esc = json.charAt(++j);
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(esc);
                }
                continue;
            }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
