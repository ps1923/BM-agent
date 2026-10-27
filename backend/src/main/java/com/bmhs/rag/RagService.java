package com.bmhs.rag;

import com.bmhs.experimentcreation.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private static final int MAX_CANDIDATES = 100;
    private final BugRepository bugRepository;
    private final EmbeddingClient embeddingClient;
    private final ChromaClient chromaClient;
    private final double maxDistance;

    @Autowired
    public RagService(BugRepository bugRepository, EmbeddingClient embeddingClient, ChromaClient chromaClient,
                      @Value("${bm-hs.rag.max-distance:1.0}") double maxDistance) {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0) {
            throw new IllegalArgumentException("RAG 最大距离阈值必须是正数");
        }
        this.bugRepository = bugRepository;
        this.embeddingClient = embeddingClient;
        this.chromaClient = chromaClient;
        this.maxDistance = maxDistance;
    }

    public RagService(BugRepository bugRepository, EmbeddingClient embeddingClient, ChromaClient chromaClient) {
        this(bugRepository, embeddingClient, chromaClient, 1.0);
    }

    public void indexApproved(RagModels.BugCaseView bug) {
        if (!"approved".equals(bug.status())) return;
        String documentId = "bug-" + bug.id();
        String document = bug.title() + "\n问题：" + bug.problem() + "\n解决：" + nullToEmpty(bug.solution());
        try {
            chromaClient.upsert(new RagModels.IndexRequest(documentId, document, bug.courseId(),
                    bug.experimentId(), bug.nodeId(), bug.technologyStack(), "approved"), embeddingClient.embed(document));
            if (!bugRepository.markVectorIndexSucceeded(bug.id(), documentId)) {
                RagModels.BugCaseView current = bugRepository.findById(bug.id());
                if ("rejected".equals(current.status())) {
                    bugRepository.markRejectedVectorPendingCleanup(bug.id());
                    current = bugRepository.findById(bug.id());
                    removeRejectedIndex(current);
                }
            }
        } catch (RuntimeException exception) {
            String code = exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName();
            logger.warn("RAG indexing failed (bugCaseId={}, code={})", bug.id(), code);
            if (!bugRepository.markVectorIndexFailed(bug.id())) {
                RagModels.BugCaseView current = bugRepository.findById(bug.id());
                if ("rejected".equals(current.status())) {
                    bugRepository.markRejectedVectorPendingCleanup(bug.id());
                    removeRejectedIndex(bugRepository.findById(bug.id()));
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${bm-hs.rag.index-retry-interval:PT5M}")
    public void retryApprovedIndexes() {
        bugRepository.findApprovedNeedingIndex(10).forEach(this::indexApproved);
        bugRepository.findRejectedNeedingVectorCleanup(10).forEach(this::removeRejectedIndex);
    }

    public void removeRejectedIndex(RagModels.BugCaseView bug) {
        if (!"rejected".equals(bug.status()) || bug.chromaDocumentId() == null) return;
        try {
            chromaClient.delete(bug.chromaDocumentId());
            if (!bugRepository.markRejectedVectorCleanupSucceeded(bug.id())) {
                RagModels.BugCaseView current = bugRepository.findById(bug.id());
                if ("approved".equals(current.status())) indexApproved(current);
            }
        } catch (RuntimeException exception) {
            String code = exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName();
            logger.warn("RAG rejected-vector cleanup failed (bugCaseId={}, code={})", bug.id(), code);
            bugRepository.markRejectedVectorCleanupFailed(bug.id());
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
            List<RagModels.ChromaHit> hits = chromaClient.query(embeddingClient.embed(query), courseId,
                    experimentId, nodeId, technologyStack, MAX_CANDIDATES);
            List<RagModels.RagReference> references = new ArrayList<>();
            for (RagModels.ChromaHit hit : hits) {
                if (!Double.isFinite(hit.distance()) || hit.distance() > maxDistance) continue;
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
                    experimentId, nodeId, technologyStack, MAX_CANDIDATES);
            List<RagModels.RagReference> references = new ArrayList<>();
            for (RagModels.ChromaHit hit : hits) {
                if (!Double.isFinite(hit.distance()) || hit.distance() > maxDistance) continue;
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
