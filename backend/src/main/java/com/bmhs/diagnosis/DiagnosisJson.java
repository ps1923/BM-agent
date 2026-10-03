package com.bmhs.diagnosis;

import com.bmhs.patch.PatchModels;
import com.google.gson.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class DiagnosisJson {
    private static final Gson GSON = new Gson();
    private DiagnosisJson() {}

    static DiagnosisModels.DiagnosisView toView(long id, long runId, String status, String json,
                                                Instant created, Instant started, Instant completed, String error) {
        Parsed parsed = parseResult(json);
        List<DiagnosisModels.PatchSummary> patches = parsed.patches().stream()
                .map(patch -> new DiagnosisModels.PatchSummary(patch.id(), patch.description(),
                        patch.files().stream().map(PatchModels.PatchFile::path).toList())).toList();
        return new DiagnosisModels.DiagnosisView(id, runId, status, parsed.summary(), parsed.findings(), patches,
                created, started, completed, error);
    }

    static String result(DiagnosisModels.Result result) { return GSON.toJson(result); }

    static Parsed parseAndValidate(String raw, java.util.Set<String> contextPaths,
                                   java.util.Set<String> patchablePaths, int maxPatches, int maxFilesPerPatch) {
        try {
            String clean = raw == null ? "" : raw.trim();
            if (clean.startsWith("```")) clean = clean.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            JsonObject root = JsonParser.parseString(clean).getAsJsonObject();
            String summary = text(root, "summary", 5000);
            if (summary.isBlank()) throw invalid();
            List<DiagnosisModels.Finding> findings = new ArrayList<>();
            JsonArray findingArray = array(root, "findings");
            if (findingArray.size() > 20) throw invalid();
            for (JsonElement element : findingArray) {
                JsonObject item = element.getAsJsonObject();
                List<String> findingFiles = strings(item, "files", 20, 300);
                if (!contextPaths.containsAll(findingFiles)) throw invalid();
                findings.add(new DiagnosisModels.Finding(text(item, "severity", 20), text(item, "title", 300),
                        text(item, "detail", 3000), findingFiles, strings(item, "verificationSteps", 10, 500)));
            }
            List<ParsedPatch> patches = new ArrayList<>();
            JsonArray patchArray = array(root, "patches");
            if (patchArray.size() > maxPatches) throw invalid();
            for (JsonElement element : patchArray) {
                JsonObject item = element.getAsJsonObject();
                String description = text(item, "description", 500);
                JsonArray files = array(item, "files");
                if (description.isBlank() || files.isEmpty() || files.size() > maxFilesPerPatch) throw invalid();
                List<PatchModels.PatchFile> payload = new ArrayList<>();
                java.util.Set<String> unique = new java.util.HashSet<>();
                for (JsonElement fileElement : files) {
                    JsonObject file = fileElement.getAsJsonObject();
                    String path = exactText(file, "path", 300);
                    String hash = exactText(file, "expectedHash", 128);
                    String content = text(file, "content", 2_000_000);
                    if (!patchablePaths.contains(path) || !unique.add(path)
                            || !hash.matches("[0-9a-fA-F]{64}") || content.isBlank()) throw invalid();
                    payload.add(new PatchModels.PatchFile(path, hash, content));
                }
                patches.add(new ParsedPatch(0L, description, List.copyOf(payload)));
            }
            return new Parsed(summary, List.copyOf(findings), List.copyOf(patches));
        } catch (RuntimeException exception) { throw invalid(); }
    }

    private static Parsed parseResult(String json) {
        if (json == null || json.isBlank()) return new Parsed("", List.of(), List.of());
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            List<DiagnosisModels.Finding> findings = new ArrayList<>();
            for (JsonElement element : array(root, "findings")) {
                JsonObject item = element.getAsJsonObject();
                findings.add(new DiagnosisModels.Finding(text(item, "severity", 20), text(item, "title", 300), text(item, "detail", 3000), strings(item, "files", 20, 300), strings(item, "verificationSteps", 10, 500)));
            }
            List<ParsedPatch> patches = new ArrayList<>();
            for (JsonElement element : array(root, "patches")) {
                JsonObject item = element.getAsJsonObject();
                List<PatchModels.PatchFile> files = new ArrayList<>();
                for (String path : strings(item, "files", 20, 300)) files.add(new PatchModels.PatchFile(path, "", ""));
                patches.add(new ParsedPatch(item.get("id").getAsLong(), text(item, "description", 500), files));
            }
            return new Parsed(text(root, "summary", 5000), List.copyOf(findings), List.copyOf(patches));
        } catch (RuntimeException ignored) { return new Parsed("", List.of(), List.of()); }
    }

    private static JsonArray array(JsonObject object, String name) { return object.has(name) && object.get(name).isJsonArray() ? object.getAsJsonArray(name) : new JsonArray(); }
    private static String text(JsonObject object, String name, int max) { if (!object.has(name) || object.get(name).isJsonNull()) return ""; String value = object.get(name).getAsString(); return value.length() > max ? value.substring(0, max) : value; }
    private static String exactText(JsonObject object, String name, int max) {
        String value = text(object, name, max + 1);
        if (value.length() > max) throw invalid();
        return value;
    }
    private static List<String> strings(JsonObject object, String name, int maxItems, int maxLength) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : array(object, name)) {
            if (result.size() >= maxItems) break;
            String value = element.getAsString();
            if (value.length() > maxLength) throw invalid();
            result.add(value);
        }
        return List.copyOf(result);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("MODEL_STRUCTURED_RESPONSE_INVALID"); }

    record Parsed(String summary, List<DiagnosisModels.Finding> findings, List<ParsedPatch> patches) {}
    record ParsedPatch(long id, String description, List<PatchModels.PatchFile> files) {}
}
