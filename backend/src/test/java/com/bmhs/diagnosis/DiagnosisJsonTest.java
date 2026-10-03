package com.bmhs.diagnosis;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosisJsonTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void acceptsFindingsAndPatchesOnlyForFilesPresentAndPatchableInPrompt() {
        String response = """
                {"summary":"原因已定位","findings":[{"severity":"medium","title":"空检查","detail":"补充检查","files":["src/Main.java"],"verificationSteps":["运行测试"]}],
                 "patches":[{"description":"补充检查","files":[{"path":"src/Main.java","expectedHash":"%s","content":"class Main { }"}]}]}
                """.formatted(HASH);

        DiagnosisJson.Parsed parsed = DiagnosisJson.parseAndValidate(response,
                Set.of("src/Main.java", "src/Large.java"), Set.of("src/Main.java"), 3, 20);

        assertEquals("原因已定位", parsed.summary());
        assertEquals("src/Main.java", parsed.patches().get(0).files().get(0).path());
    }

    @Test
    void rejectsPatchAgainstTruncatedFileEvenWhenItsHashIsWellFormed() {
        String response = """
                {"summary":"原因已定位","findings":[],"patches":[{"description":"修改","files":[{"path":"src/Large.java","expectedHash":"%s","content":"class Large { }"}]}]}
                """.formatted(HASH);

        assertThrows(IllegalArgumentException.class, () -> DiagnosisJson.parseAndValidate(response,
                Set.of("src/Large.java"), Set.of(), 3, 20));
    }

    @Test
    void rejectsModelFindingsThatCiteFilesOutsideTheSuppliedProject() {
        String response = """
                {"summary":"原因已定位","findings":[{"severity":"high","title":"问题","detail":"描述","files":["../secrets.yml"],"verificationSteps":[]}],"patches":[]}
                """;

        assertThrows(IllegalArgumentException.class, () -> DiagnosisJson.parseAndValidate(response,
                Set.of("src/Main.java"), Set.of("src/Main.java"), 3, 20));
    }

    @Test
    void rejectsOverlongPatchPathInsteadOfTruncatingItToAnAllowedPath() {
        String allowedPath = "a".repeat(300);
        String response = """
                {"summary":"原因已定位","findings":[],"patches":[{"description":"修改","files":[{"path":"%s","expectedHash":"%s","content":"class Main { }"}]}]}
                """.formatted(allowedPath + "tail", HASH);

        assertThrows(IllegalArgumentException.class, () -> DiagnosisJson.parseAndValidate(response,
                Set.of(allowedPath), Set.of(allowedPath), 3, 20));
    }
}
