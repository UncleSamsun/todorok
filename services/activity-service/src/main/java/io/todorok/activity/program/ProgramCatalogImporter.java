package io.todorok.activity.program;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class ProgramCatalogImporter {
    public record Session(int session, List<Integer> sets, int targetTotal) {}
    public record Week(int week, List<Session> sessions) {}
    public record Source(String kind, String label, String url, List<String> conditions, List<String> cautions) {}
    public record Catalog(String catalogKey, long version, String checksum, String name, Source source, int sessionsPerWeek, List<Week> weeks) {}

    private final ObjectMapper mapper;
    public ProgramCatalogImporter(ObjectMapper mapper) { this.mapper = mapper; }

    public Catalog read(Path source) {
        try { return parse(mapper.readTree(java.nio.file.Files.readString(source))); }
        catch (IOException exception) { throw new IllegalArgumentException("Cannot read program catalog", exception); }
    }

    public Catalog parse(JsonNode root) {
        if (root == null || !root.isObject()) throw invalid("Catalog must be an object.");
        String key = requiredText(root, "catalogKey", 120);
        long version = positive(root, "version");
        String checksum = requiredText(root, "checksum", 64);
        if (!checksum.matches("[a-f0-9]{64}") || !checksum.equals(checksum(root))) throw invalid("Catalog checksum does not match its canonical content.");
        requiredText(root, "name", 120);
        var source = parseSource(root.get("source"));
        int sessionsPerWeek = Math.toIntExact(positive(root, "sessionsPerWeek"));
        if (sessionsPerWeek > 7) throw invalid("sessionsPerWeek must not exceed seven.");
        var weeksNode = root.get("weeks");
        if (weeksNode == null || !weeksNode.isArray() || weeksNode.isEmpty()) throw invalid("Catalog requires weeks.");
        var weeks = new ArrayList<Week>();
        int expectedWeek = 1;
        for (var weekNode : weeksNode) {
            if (!weekNode.isObject() || positive(weekNode, "week") != expectedWeek++) throw invalid("Weeks must be contiguous from one.");
            var sessionsNode = weekNode.get("sessions");
            if (sessionsNode == null || !sessionsNode.isArray() || sessionsNode.size() != sessionsPerWeek) throw invalid("Every week must contain the declared number of sessions.");
            var sessions = new ArrayList<Session>();
            int expectedSession = 1;
            for (var sessionNode : sessionsNode) {
                if (!sessionNode.isObject() || positive(sessionNode, "session") != expectedSession++) throw invalid("Sessions must be contiguous from one.");
                var setsNode = sessionNode.get("sets");
                if (setsNode == null || !setsNode.isArray() || setsNode.isEmpty()) throw invalid("A session requires sets.");
                var sets = new ArrayList<Integer>();
                int sum = 0;
                for (var set : setsNode) { int repetitions = Math.toIntExact(positiveValue(set, "set")); sets.add(repetitions); sum += repetitions; }
                int target = Math.toIntExact(positive(sessionNode, "targetTotal"));
                if (sum != target) throw invalid("targetTotal must equal the sum of sets.");
                sessions.add(new Session(expectedSession - 1, List.copyOf(sets), target));
            }
            weeks.add(new Week(expectedWeek - 1, List.copyOf(sessions)));
        }
        return new Catalog(key, version, checksum, requiredText(root, "name", 120), source, sessionsPerWeek, List.copyOf(weeks));
    }

    public Source parseSource(JsonNode source) {
        if (source == null || !source.isObject()) throw invalid("Catalog source is required.");
        String kind = requiredText(source, "kind", 32);
        if (!Set.of("SYNTHETIC", "PRIVATE_VERIFIED").contains(kind)) throw invalid("Catalog source kind is invalid.");
        String url = optionalUrl(source), label = requiredText(source, "label", 200);
        var conditions = optionalTexts(source, "conditions");
        var cautions = optionalTexts(source, "cautions");
        if (kind.equals("PRIVATE_VERIFIED") && (url == null || conditions.isEmpty() || cautions.isEmpty()))
            throw invalid("Private verified catalog requires source url, conditions, and cautions.");
        return new Source(kind, label, url, conditions, cautions);
    }

    public String checksum(JsonNode root) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(root, true).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private String canonical(JsonNode node, boolean root) {
        if (node.isObject()) {
            var keys = new TreeSet<String>(); node.propertyNames().forEach(keys::add);
            if (root) keys.remove("checksum");
            return "{" + keys.stream().map(key -> mapper.writeValueAsString(key) + ":" + canonical(node.get(key), false)).collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (node.isArray()) { var values = new ArrayList<String>(); node.forEach(value -> values.add(canonical(value, false))); return "[" + String.join(",", values) + "]"; }
        return node.toString();
    }
    private String requiredText(JsonNode node, String field, int max) {
        var value = node.get(field); if (value == null || !value.isTextual() || value.asText().isBlank() || value.asText().length() > max) throw invalid("Missing or invalid " + field + "."); return value.asText();
    }
    private String optionalUrl(JsonNode source) {
        var value = source.get("url");
        if (value == null) return null;
        String url = requiredText(source, "url", 2048);
        try {
            var parsed = URI.create(url);
            if (!Set.of("http", "https").contains(parsed.getScheme()) || parsed.getHost() == null) throw invalid("Invalid source url.");
            return url;
        } catch (IllegalArgumentException exception) { throw invalid("Invalid source url."); }
    }
    private List<String> optionalTexts(JsonNode source, String field) {
        var values = source.get(field);
        if (values == null) return List.of();
        if (!values.isArray()) throw invalid("Invalid " + field + ".");
        var result = new ArrayList<String>();
        for (var value : values) {
            if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 500) throw invalid("Invalid " + field + ".");
            result.add(value.asText());
        }
        return List.copyOf(result);
    }
    private long positive(JsonNode node, String field) {
        return positiveValue(node.get(field), field);
    }
    private long positiveValue(JsonNode value, String field) { if (value == null || !value.isIntegralNumber() || value.longValue() < 1) throw invalid("Missing or invalid " + field + "."); return value.longValue(); }
    private IllegalArgumentException invalid(String detail) { return new IllegalArgumentException(detail); }
}
