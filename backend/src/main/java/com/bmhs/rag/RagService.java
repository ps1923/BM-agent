package com.bmhs.rag;

import com.bmhs.experimentcreation.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final BugRepository bugRepository;
    private final EmbeddingClient embeddingClient;
    private final ChromaClient chromaClient;

    public RagService(BugRepository bugRepository, EmbeddingClient embeddingClient, ChromaClient chromaClient) {
        this.bugRepository = bugRepository;
        this.embeddingClient = embeddingClient;
        this.chromaClient = chromaClient;
    }

    public void indexApproved(RagModels.BugCaseView bug) {
        if (!"approved".equals(bug.status())) return;
        String documentId = "bug-" + bug.id();
        String document = bug.title() + "\n问题：" + bug.problem() + "\n解决：" + nullToEmpty(bug.solution());
        try {
            chromaClient.upsert(new RagModels.IndexRequest(documentId, document, bug.courseId(),
                    bug.experimentId(), bug.nodeId(), bug.technologyStack(), "approved"), embeddingClient.embed(document));
            bugRepository.updateVectorStatus(bug.id(), "indexed", documentId);
        } catch (ApiException exception) {
            logger.warn("RAG indexing failed (bugCaseId={}, code={})", bug.id(), exception.code());
            bugRepository.updateVectorStatus(bug.id(), "failed", null);
        }
    }

    public RagModels.RagSearchResult searchForStudent(long studentId, Long courseId, String query) {
        return searchForStudent(studentId, courseId, null, null, query);
    }

    public RagModels.RagSearchResult searchForStudent(long studentId, Long courseId, Long experimentId,
                                                       Long nodeId, String query) {
        return searchForStudent(studentId, courseId, experimentId, nodeId, null, query);
    }

    public RagModels.RagSearchResult searchForStudent(long studentId, Long courseId, Long experimentId,
                                                       Long nodeId, String technologyStack, String query) {
        if (experimentId == null) return new RagModels.RagSearchResult(false, List.of());
        if (courseId == null) {
            return searchForExperiment(studentId, experimentId, nodeId, technologyStack, query);
        }
        try {
            List<RagModels.ChromaHit> hits = chromaClient.query(embeddingClient.embed(query), courseId, experimentId,
                    technologyStack, 8);
            List<RagModels.RagReference> references = new ArrayList<>();
            for (RagModels.ChromaHit hit : hits) {
                if (!hit.id().startsWith("bug-")) continue;
                try {
                    long bugId = Long.parseLong(hit.id().substring(4));
                    RagModels.BugCaseView bug = bugRepository.findVisibleByStudent(bugId, studentId);
                    boolean nodeCompatible = nodeId == null || bug.nodeId() == null || nodeId.equals(bug.nodeId());
                    boolean stackCompatible = technologyStack == null || technologyStack.isBlank()
                            || bug.technologyStack() == null || technologyStack.equalsIgnoreCase(bug.technologyStack());
                    if ("approved".equals(bug.status()) && courseId.equals(bug.courseId())
                            && experimentId.equals(bug.experimentId()) && nodeCompatible && stackCompatible) {
                        references.add(new RagModels.RagReference(bug.id(), bug.title(), bug.problem(), bug.solution(), hit.distance()));
                    }
                } catch (NumberFormatException | ApiException ignored) {
                    // Ignore malformed vector identifiers; the database remains authoritative.
                }
            }
            return new RagModels.RagSearchResult(true, List.copyOf(references.stream().limit(3).toList()));
        } catch (ApiException exception) {
            return new RagModels.RagSearchResult(false, List.of());
        }
    }

    public RagModels.RagSearchResult searchForExperiment(long studentId, long experimentId, Long nodeId,
                                                          String technologyStack, String query) {
        return searchForExperiment(studentId, experimentId, nodeId, technologyStack, query, 3);
    }

    public RagModels.RagSearchResult searchForExperiment(long studentId, long experimentId, Long nodeId,
                                                          String technologyStack, String query, int limit) {
        int resultLimit = Math.max(1, Math.min(limit, 10));
        try {
            List<RagModels.ChromaHit> hits = chromaClient.queryByExperiment(embeddingClient.embed(query), null,
                    experimentId, technologyStack, Math.max(8, resultLimit));
            List<RagModels.RagReference> references = new ArrayList<>();
            for (RagModels.ChromaHit hit : hits) {
                if (!hit.id().startsWith("bug-")) continue;
                try {
                    long bugId = Long.parseLong(hit.id().substring(4));
                    RagModels.BugCaseView bug = bugRepository.findApprovedForExperiment(bugId, studentId, experimentId);
                    boolean nodeCompatible = nodeId == null || bug.nodeId() == null || nodeId.equals(bug.nodeId());
                    boolean stackCompatible = technologyStack == null || technologyStack.isBlank()
                            || bug.technologyStack() == null || technologyStack.equalsIgnoreCase(bug.technologyStack());
                    if (nodeCompatible && stackCompatible) {
                        references.add(new RagModels.RagReference(bug.id(), bug.title(), bug.problem(), bug.solution(), hit.distance()));
                    }
                } catch (NumberFormatException | ApiException ignored) {
                    // The vector index is only a candidate source; MySQL validates visibility and scope.
                }
            }
            return new RagModels.RagSearchResult(true, List.copyOf(references.stream().limit(resultLimit).toList()));
        } catch (ApiException exception) {
            return new RagModels.RagSearchResult(false, List.of());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
