package com.chuseok22.whoim.server.admin.application.service.table;

import com.chuseok22.whoim.server.admin.application.dto.response.AdminCell;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminDetailResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminFieldResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminListResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminRowResponse;
import com.chuseok22.whoim.server.admin.application.dto.response.AdminSectionResponse;
import com.chuseok22.whoim.server.admin.application.service.AdminPaging;
import com.chuseok22.whoim.server.admin.core.constant.AdminTable;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.interview.infrastructure.entity.Interview;
import com.chuseok22.whoim.server.interview.infrastructure.entity.InterviewTurn;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewRepository;
import com.chuseok22.whoim.server.interview.infrastructure.repository.InterviewTurnRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterviewTableView implements AdminTableView {

  private static final List<String> COLUMNS = List.of(
      "id", "handover_package_id", "gap_id", "status", "end_reason", "trace_state", "created_at");
  private static final List<String> TURN_COLUMNS = List.of("turn", "question", "answer", "extracted_trace");
  private static final String CONVERSATION_KIND = "CONVERSATION";
  private static final String NO_ANSWER = "(답변 없음)";

  private final InterviewRepository interviewRepository;
  private final InterviewTurnRepository interviewTurnRepository;
  private final AdminPackageOptions adminPackageOptions;

  @Override
  public AdminTable table() {
    return AdminTable.INTERVIEW;
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return interviewRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public Instant lastCreatedAt() {
    return interviewRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
        .stream().findFirst().map(Interview::getCreatedAt).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AdminListResponse list(int page, UUID handoverPackageId) {
    Page<Interview> interviews = handoverPackageId == null
        ? interviewRepository.findAll(AdminPaging.pageRequest(page))
        : interviewRepository.findAllByHandoverPackageId(handoverPackageId, AdminPaging.pageRequest(page));
    List<AdminRowResponse> rows = interviews.getContent().stream()
        .map(interview -> new AdminRowResponse(interview.getId(), List.of(
            AdminCell.link(AdminTable.INTERVIEW, interview.getId()),
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, interview.getHandoverPackage().getId()),
            AdminCell.link(AdminTable.GAP, interview.getGap().getId()),
            AdminCell.badge(interview.getStatus().name()),
            AdminCell.badge(enumName(interview.getEndReason())),
            AdminCell.truncated(interview.getTraceState()),
            AdminCell.time(interview.getCreatedAt()))))
        .toList();
    return new AdminListResponse(AdminTable.INTERVIEW.pathName(), COLUMNS, rows, interviews.getNumber(),
        interviews.getTotalPages(), interviews.getTotalElements(), handoverPackageId,
        adminPackageOptions.optionsFor(AdminTable.INTERVIEW));
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDetailResponse detail(UUID id) {
    Interview interview = interviewRepository.findById(id)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    List<AdminRowResponse> turnRows = interviewTurnRepository.findAllByInterviewOrderByTurnNumberAsc(interview)
        .stream()
        .map(this::turnRow)
        .toList();
    return new AdminDetailResponse(AdminTable.INTERVIEW.pathName(), interview.getId(), List.of(
        new AdminFieldResponse("id", AdminCell.text(interview.getId().toString())),
        new AdminFieldResponse("handover_package_id",
            AdminCell.link(AdminTable.HANDOVER_PACKAGE, interview.getHandoverPackage().getId())),
        new AdminFieldResponse("gap_id", AdminCell.link(AdminTable.GAP, interview.getGap().getId())),
        new AdminFieldResponse("status", AdminCell.badge(interview.getStatus().name())),
        new AdminFieldResponse("end_reason", AdminCell.badge(enumName(interview.getEndReason()))),
        new AdminFieldResponse("trace_state", AdminCell.text(interview.getTraceState())),
        new AdminFieldResponse("created_at", AdminCell.time(interview.getCreatedAt())),
        new AdminFieldResponse("updated_at", AdminCell.time(interview.getUpdatedAt()))),
        List.of(new AdminSectionResponse("턴 (interview_turn)", CONVERSATION_KIND, TURN_COLUMNS, turnRows)));
  }

  // 셀 순서는 detail.html의 CONVERSATION 레이아웃과 맞춘다: 헤더, 질문, 답변, 추출
  private AdminRowResponse turnRow(InterviewTurn turn) {
    String header = "TURN " + turn.getTurnNumber()
        + " · " + AdminCell.plainId(turn.getId()).text()
        + " · " + AdminCell.time(turn.getCreatedAt()).text();
    return new AdminRowResponse(turn.getId(), List.of(
        AdminCell.text(header),
        AdminCell.text(turn.getQuestion()),
        AdminCell.text(turn.getAnswer() == null ? NO_ANSWER : turn.getAnswer()),
        AdminCell.text(turn.getExtractedTrace())));
  }

  private String enumName(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
