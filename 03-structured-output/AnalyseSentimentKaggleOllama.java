// Java port of analyse_sentiment_kaggle_ollama.py.
//
// Single-file program — no Maven, no dependencies. Run with:
//
//   java AnalyseSentimentKaggleOllama.java
//
// Requires Java 11+ (uses java.net.http.HttpClient and source-file launching).
//
// The Trump-tweets CSV is read from the kagglehub cache populated by the
// Python version, so run `python analyse_sentiment_kaggle_ollama.py` once
// first to download the dataset. Ollama must be running locally with the
// qwen3.5:4b model pulled.

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class AnalyseSentimentKaggleOllama {

    static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    static final String MODEL = "qwen3.5:4b";
    static final int SAMPLE_SIZE = 20;

    static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        Path csv = findKaggleCsv();
        if (csv == null) {
            System.err.println("Kaggle CSV not found in ~/.cache/kagglehub/.");
            System.err.println("Run `python analyse_sentiment_kaggle_ollama.py` first.");
            return;
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
