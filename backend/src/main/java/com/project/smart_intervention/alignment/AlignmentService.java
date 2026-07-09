package com.project.smart_intervention.alignment;

import com.project.smart_intervention.entity.pojo.PersonalAlignmentState;
import com.project.smart_intervention.entity.pojo.RelationshipAlignmentState;
import com.project.smart_intervention.entity.request.AlignmentFeedbackRequest;
import com.project.smart_intervention.entity.request.AlignmentTurnRequest;
import com.project.smart_intervention.entity.response.AlignmentTurnResponse;

public interface AlignmentService {
    AlignmentTurnResponse recordTurn(AlignmentTurnRequest request);

    PersonalAlignmentState getPersonalState(String userId);

    RelationshipAlignmentState getRelationshipState(String userId, String counterpartId);

    void recordFeedback(AlignmentFeedbackRequest request);
}
