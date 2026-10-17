package com.chuseok22.whoim.server.gap.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
import com.chuseok22.whoim.server.gap.application.dto.data.ConvertedGap;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisInput;
import com.chuseok22.whoim.server.gap.application.dto.data.GapAnalysisResult;
import com.chuseok22.whoim.server.gap.application.dto.response.GapResponse;
import com.chuseok22.whoim.server.gap.core.constant.GapProgress;
import com.chuseok22.whoim.server.gap.infrastructure.entity.Gap;
import com.chuseok22.whoim.server.gap.infrastructure.entity.GapAnchor;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapAnchorRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.llm.application.service.LlmService;
import com.chuseok22.whoim.server.member.core.constant.Position;
import com.chuseok22.whoim.server.setting.application.service.SettingConfigService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// LLM 호출이 트랜잭션 안에 들어가지 않도록 @Transactional 대신 읽기/쓰기 템플릿을 나눠 쓴다
@Slf4j
@Service
public class GapAnalysisService {

  public static final String MAX_INPUT_CHARACTERS_KEY = "gap.analysis.max-input-characters";
  public static final int DEFAULT_MAX_INPUT_CHARACTERS = 20000;

  private final HandoverPackageRepository handoverPackageRepository;
  private final EvidenceDocumentRepository evidenceDocumentRepository;
  private final GapRepository gapRepository;
  private final GapAnchorRepository gapAnchorRepository;
  private final GapAnalysisInputBuilder gapAnalysisInputBuilder;
  private final GapAnalysisResultConverter gapAnalysisResultConverter;
  private final LlmService llmService;
  private final SettingConfigService settingConfigService;
  private final TransactionTemplate readTransaction;
  private final TransactionTemplate writeTransaction;

  public GapAnalysisService(
      HandoverPackageRepository handoverPackageRepository,
      EvidenceDocumentRepository evidenceDocumentRepository,
      GapRepository gapRepository,
      GapAnchorRepository gapAnchorRepository,
      GapAnalysisInputBuilder gapAnalysisInputBuilder,
      GapAnalysisResultConverter gapAnalysisResultConverter,
      LlmService llmService,
      SettingConfigService settingConfigService,
      PlatformTransactionManager transactionManager) {
    this.handoverPackageRepository = handoverPackageRepository;
    this.evidenceDocumentRepository = evidenceDocumentRepository;
    this.gapRepository = gapRepository;
    this.gapAnchorRepository = gapAnchorRepository;
    this.gapAnalysisInputBuilder = gapAnalysisInputBuilder;
    this.gapAnalysisResultConverter = gapAnalysisResultConverter;
    this.llmService = llmService;
    this.settingConfigService = settingConfigService;
    this.readTransaction = new TransactionTemplate(transactionManager);
    this.readTransaction.setReadOnly(true);
    this.writeTransaction = new TransactionTemplate(transactionManager);
  }

  // 검증 순서: 권한 -> 소유 -> Package 상태 -> Gap 없음 -> 문서 -> 입력 상한
  public List<GapResponse> analyze(AuthenticatedMember requester, UUID handoverPackageId) {
    GapAnalysisInput input = readTransaction.execute(status -> prepareInput(requester, handoverPackageId));

    GapAnalysisResult result = llmService.generate(GapAnalysisPrompt.build(input.text()), GapAnalysisResult.class);

    return writeTransaction.execute(status -> saveResult(handoverPackageId, input, result));
  }

  private GapAnalysisInput prepareInput(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (Position.PREDECESSOR != requester.position()
        || !handoverPackage.getPredecessor().getId().equals(requester.memberId())) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    if (handoverPackage.getStatus() != HandoverStatus.WRITING || gapRepository.existsByHandoverPackage(handoverPackage)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }
    List<EvidenceDocument> documents = evidenceDocumentRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage);
    if (documents.isEmpty()) {
      throw new CustomException(ErrorCode.GAP_ANALYSIS_NO_DOCUMENTS);
    }
    GapAnalysisInput input = gapAnalysisInputBuilder.build(documents);
    int maxInputCharacters = settingConfigService.getInt(MAX_INPUT_CHARACTERS_KEY, DEFAULT_MAX_INPUT_CHARACTERS);
    if (input.text().length() > maxInputCharacters) {
      throw new CustomException(ErrorCode.GAP_ANALYSIS_INPUT_TOO_LARGE);
    }
    return input;
  }

  // 잠금 후 재검증: 같은 Package의 중복 분석과 분석 중 문서 업로드를 막는다
  private List<GapResponse> saveResult(UUID handoverPackageId, GapAnalysisInput input, GapAnalysisResult result) {
    HandoverPackage handoverPackage = handoverPackageRepository.findWithLockById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    if (handoverPackage.getStatus() != HandoverStatus.WRITING
        || gapRepository.existsByHandoverPackage(handoverPackage)
        || evidenceDocumentRepository.countByHandoverPackage(handoverPackage) != input.documents().size()) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }

    List<ConvertedGap> convertedGaps = gapAnalysisResultConverter.convert(result, input.documents());
    List<GapResponse> responses = new ArrayList<>();
    for (ConvertedGap convertedGap : convertedGaps) {
      Gap gap = gapRepository.save(Gap.builder()
          .handoverPackage(handoverPackage)
          .type(convertedGap.type())
          .summary(convertedGap.summary())
          .question(convertedGap.question())
          .build());
      List<GapAnchor> anchors = gapAnchorRepository.saveAll(convertedGap.anchors().stream()
          .map(anchor -> GapAnchor.builder()
              .gap(gap)
              .document(anchor.document())
              .startOffset(anchor.startOffset())
              .endOffset(anchor.endOffset())
              .excerpt(anchor.excerpt())
              .build())
          .toList());
      responses.add(GapResponse.from(gap, anchors, GapProgress.NOT_STARTED));
    }
    log.info("Gap 분석 저장: handoverPackageId={}, gapCount={}", handoverPackageId, responses.size());
    return responses;
  }
}
