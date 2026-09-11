package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfigurationRequest;
import com.bmhs.experimentcreation.CreationModels.ConfirmedIntent;
import com.bmhs.experimentcreation.CreationModels.CreateSessionResponse;
import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.MaterializedExperiment;
import com.bmhs.experimentcreation.CreationModels.PlanResponse;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import com.bmhs.experimentcreation.CreationModels.SessionView;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExperimentCreationService {
    private final CreationRepository repository;
    private final HarnessGateway harness;
    private final ExperimentTreeValidator validator;
    private final TreeLayoutService layoutService;
    private final CreationTransactions transactions;
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public ExperimentCreationService(CreationRepository repository, HarnessGateway harness,
                                     ExperimentTreeValidator validator, TreeLayoutService layoutService,
                                     CreationTransactions transactions) {
        this.repository = repository;
        this.harness = harness;
        this.validator = validator;
        this.layoutService = layoutService;
        this.transactions = transactions;
    }

    public CreateSessionResponse createSession(long userId) {
        var session = repository.create(userId);
        return new CreateSessionResponse(session.id(), session.status());
    }

    public StudentIntentEnvelope sendDirectionMessage(String sessionId, long userId, String message) {
        var session = transactions.snapshot(sessionId, userId);
        requireStatus(session.status(), "direction_discussion", "direction_ready");
        StudentIntentEnvelope result = harness.clarify(message, session.direction(), false);
        if (!List.of("needs_clarification", "ready_for_confirmation").contains(result.status())) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_ROLE_VIOLATION",
                    "需求 Harness 返回了不允许的状态");
        }
        validateDirection(result);
        return transactions.saveDirectionTurn(session, message, result);
    }

    @Transactional
    public ConfirmedIntent confirmDirection(String sessionId, long userId) {
        var session = repository.lock(sessionId, userId);
        requireStatus(session.status(), "direction_ready", "direction_confirmed");
        if (session.confirmedIntent() != null) return session.confirmedIntent();
        StudentIntentEnvelope result = new StudentIntentEnvelope(
                session.direction().schemaVersion(), "confirmed",
                "实验方向已经确认，接下来请选择学习周期和难度。",
                session.direction().intent(), session.direction().fieldEvidence(),
                List.of(), session.direction().assumptions(), null);
        String hash = sha256(gson.toJson(session.direction().intent()));
        ConfirmedIntent confirmed = new ConfirmedIntent("0.1", hash, Instant.now().toString(),
                session.direction().intent());
        repository.confirmDirection(sessionId, result, confirmed, hash);
        return confirmed;
    }

    @Transactional
    public SessionView configure(String sessionId, long userId, ConfigurationRequest request) {
        if (request.durationMinutes() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DURATION", "学习周期必须大于 0");
        }
        var session = repository.lock(sessionId, userId);
        requireStatus(session.status(), "direction_confirmed", "configured", "plan_ready");
        repository.configure(sessionId, request.durationMinutes(), request.difficulty());
        return view(repository.find(sessionId, userId));
    }

    public PlanResponse generatePlan(String sessionId, long userId) {
        var session = transactions.snapshot(sessionId, userId);
        requireStatus(session.status(), "configured", "plan_ready");
        if (session.confirmedIntent() == null || session.durationMinutes() == null || session.difficulty() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_NOT_CONFIGURED", "请先确认方向、周期和难度");
        }
        ExperimentTree tree = generateValidTree(session);
        List<PositionedNode> nodes = layoutService.layout(tree);
        return transactions.savePlan(session, tree.title(), tree.purpose(), nodes);
    }

    private ExperimentTree generateValidTree(CreationRepository.SessionRecord session) {
        ExperimentTree tree = harness.generateTree(
                session.confirmedIntent(), session.durationMinutes(), session.difficulty());
        validator.validate(tree, session.durationMinutes());
        return tree;
    }

    @Transactional
    public MaterializedExperiment materialize(String sessionId, long userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "需要有效的幂等键");
        }
        var session = repository.lock(sessionId, userId);
        if ("materialized".equals(session.status())) {
            if (session.experimentId() == null) {
                throw new ApiException(HttpStatus.GONE, "MATERIALIZED_EXPERIMENT_MISSING",
                        "已创建的实验不存在");
            }
            if (!idempotencyKey.equals(session.materializationKey())) {
                throw new ApiException(HttpStatus.CONFLICT, "SESSION_ALREADY_MATERIALIZED", "该会话已经创建过实验");
            }
            return response(session.experimentId(), session);
        }
        requireStatus(session.status(), "plan_ready");
        if (session.plan() == null || !session.plan().intentHash().equals(session.intentHash())) {
            throw new ApiException(HttpStatus.CONFLICT, "STALE_PLAN", "计划版本已过期，请重新生成");
        }

        String learningGoal = intentText(session.confirmedIntent().intent(), "observable_outcome", "raw_request");
        String description = String.join("；", session.plan().purpose());
        long experimentId = repository.insertExperiment(userId, session.plan().title(), learningGoal, description,
                session.difficulty(), session.durationMinutes(), gson.toJson(Map.of(
                        "intentHash", session.intentHash(), "planId", session.plan().planId(),
                        "planVersion", session.plan().version(), "generator", "planner-harness")));

        Map<String, Long> stageIds = new LinkedHashMap<>();
        session.plan().nodes().stream()
                .sorted(Comparator.comparingInt(PositionedNode::depth))
                .map(PositionedNode::stage)
                .distinct()
                .forEach(stage -> stageIds.put(stage,
                        repository.insertStage(experimentId, stage, stageIds.size())));

        Map<String, Long> nodeIds = new LinkedHashMap<>();
        List<PositionedNode> ordered = session.plan().nodes().stream()
                .sorted(Comparator.comparingInt(PositionedNode::depth).thenComparing(PositionedNode::key))
                .toList();
        for (int index = 0; index < ordered.size(); index++) {
            PositionedNode node = ordered.get(index);
            Long parentId = node.parentKey() == null ? null : nodeIds.get(node.parentKey());
            long nodeId = repository.insertNode(experimentId, stageIds.get(node.stage()), parentId, node, index);
            nodeIds.put(node.key(), nodeId);
            for (int taskIndex = 0; taskIndex < node.tasks().size(); taskIndex++) {
                repository.insertTask(experimentId, nodeId, node.tasks().get(taskIndex), taskIndex);
            }
            if (parentId != null) repository.insertDependency(experimentId, parentId, nodeId);
        }
        repository.finishMaterialization(sessionId, session.plan().planId(), idempotencyKey, experimentId);
        return new MaterializedExperiment(experimentId, session.plan().title(), description,
                session.difficulty().label(), session.durationMinutes(), session.plan().nodes());
    }

    public SessionView get(String sessionId, long userId) {
        return view(repository.find(sessionId, userId));
    }

    private MaterializedExperiment response(long experimentId, CreationRepository.SessionRecord session) {
        String description = session.plan() == null ? "" : String.join("；", session.plan().purpose());
        return new MaterializedExperiment(experimentId,
                session.plan() == null ? "已创建实验" : session.plan().title(), description,
                session.difficulty() == null ? "" : session.difficulty().label(),
                session.durationMinutes() == null ? 0 : session.durationMinutes(),
                session.plan() == null ? List.of() : session.plan().nodes());
    }

    private SessionView view(CreationRepository.SessionRecord session) {
        return new SessionView(session.id(), session.status(), session.direction(), session.durationMinutes(),
                session.difficulty(), session.plan(), session.experimentId());
    }

    private void requireStatus(String actual, String... allowed) {
        if (List.of(allowed).contains(actual)) return;
        throw new ApiException(HttpStatus.CONFLICT, "INVALID_SESSION_STATE",
                "当前状态 " + actual + " 不能执行该操作");
    }

    private String intentText(Map<String, Object> intent, String... keys) {
        for (String key : keys) {
            Object value = intent.get(key);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return "完成已确认的实验目标";
    }

    private void validateDirection(StudentIntentEnvelope result) {
        boolean invalid = result == null || !"0.1".equals(result.schemaVersion())
                || result.assistantMessage() == null || result.assistantMessage().isBlank()
                || result.intent() == null || gson.toJson(result).length() > 64_000;
        if (!invalid && "needs_clarification".equals(result.status())) {
            invalid = result.nextQuestion() == null || result.nextQuestion().text() == null
                    || result.nextQuestion().text().isBlank();
        }
        if (!invalid && "ready_for_confirmation".equals(result.status())) {
            invalid = result.nextQuestion() != null
                    || (result.missingCriticalFields() != null && !result.missingCriticalFields().isEmpty());
        }
        if (invalid) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "HARNESS_OUTPUT_INVALID",
                    "需求 Harness 返回结果不符合确认协议");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
