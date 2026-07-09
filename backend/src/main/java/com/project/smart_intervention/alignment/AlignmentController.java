package com.project.smart_intervention.alignment;

import com.project.smart_intervention.entity.pojo.PersonalAlignmentState;
import com.project.smart_intervention.entity.pojo.RelationshipAlignmentState;
import com.project.smart_intervention.entity.pojo.Result;
import com.project.smart_intervention.entity.request.AlignmentFeedbackRequest;
import com.project.smart_intervention.entity.request.AlignmentTurnRequest;
import com.project.smart_intervention.entity.response.AlignmentTurnResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/aico/alignment")
public class AlignmentController {
    @Resource
    private AlignmentService alignmentService;

    @PostMapping("/turns")
    public Result<AlignmentTurnResponse> recordTurn(@RequestBody AlignmentTurnRequest request) {
        return Result.success(alignmentService.recordTurn(request));
    }

    @GetMapping("/users/{userId}/state")
    public Result<PersonalAlignmentState> getPersonalState(@PathVariable String userId) {
        return Result.success(alignmentService.getPersonalState(userId));
    }

    @GetMapping("/relationships")
    public Result<RelationshipAlignmentState> getRelationshipState(
            @RequestParam String userId,
            @RequestParam String counterpartId
    ) {
        return Result.success(alignmentService.getRelationshipState(userId, counterpartId));
    }

    @PostMapping("/feedback")
    public Result<String> recordFeedback(@RequestBody AlignmentFeedbackRequest request) {
        alignmentService.recordFeedback(request);
        return Result.success("success");
    }
}
