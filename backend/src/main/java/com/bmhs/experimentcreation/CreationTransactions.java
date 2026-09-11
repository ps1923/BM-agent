package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.PlanResponse;
import com.bmhs.experimentcreation.CreationModels.PositionedNode;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CreationTransactions {
    private final CreationRepository repository;

    public CreationTransactions(CreationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CreationRepository.SessionRecord snapshot(String sessionId, long userId) {
        return repository.find(sessionId, userId);
    }

    @Transactional
    public StudentIntentEnvelope saveDirectionTurn(CreationRepository.SessionRecord snapshot,
                                                   String message, StudentIntentEnvelope result) {
        var current = repository.lock(snapshot.id(), snapshot.userId());
        if (current.intentVersion() != snapshot.intentVersion()
                || !Objects.equals(current.status(), snapshot.status())) {
            stale();
        }
        repository.appendMessage(snapshot.id(), "user", message);
        repository.appendMessage(snapshot.id(), "assistant", result.assistantMessage());
        repository.saveDirection(snapshot.id(), result);
        return result;
    }

    @Transactional
    public PlanResponse savePlan(CreationRepository.SessionRecord snapshot, String title,
                                 List<String> purpose, List<PositionedNode> nodes) {
        var current = repository.lock(snapshot.id(), snapshot.userId());
        String snapshotPlanId = snapshot.plan() == null ? null : snapshot.plan().planId();
        String currentPlanId = current.plan() == null ? null : current.plan().planId();
        if (!Objects.equals(current.status(), snapshot.status())
                || !Objects.equals(current.intentHash(), snapshot.intentHash())
                || !Objects.equals(current.durationMinutes(), snapshot.durationMinutes())
                || current.difficulty() != snapshot.difficulty()
                || !Objects.equals(currentPlanId, snapshotPlanId)) {
            stale();
        }
        PlanResponse draft = new PlanResponse(null, 0, snapshot.intentHash(), title, purpose, nodes);
        return repository.savePlan(snapshot.id(), snapshot.intentHash(), draft);
    }

    private void stale() {
        throw new ApiException(HttpStatus.CONFLICT, "SESSION_CHANGED",
                "创建会话已被其他请求更新，请刷新后重试");
    }
}
