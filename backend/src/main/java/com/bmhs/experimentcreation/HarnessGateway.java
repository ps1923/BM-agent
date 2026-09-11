package com.bmhs.experimentcreation;

import com.bmhs.experimentcreation.CreationModels.ConfirmedIntent;
import com.bmhs.experimentcreation.CreationModels.Difficulty;
import com.bmhs.experimentcreation.CreationModels.ExperimentTree;
import com.bmhs.experimentcreation.CreationModels.StudentIntentEnvelope;

public interface HarnessGateway {
    StudentIntentEnvelope clarify(String message, StudentIntentEnvelope current, boolean confirm);

    ExperimentTree generateTree(ConfirmedIntent intent, int durationMinutes, Difficulty difficulty);
}
