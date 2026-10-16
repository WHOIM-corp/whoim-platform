package com.chuseok22.whoim.server.evidence.application.service;

import com.chuseok22.whoim.server.auth.application.dto.data.AuthenticatedMember;
import com.chuseok22.whoim.server.common.application.exception.CustomException;
import com.chuseok22.whoim.server.common.application.exception.ErrorCode;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentDetailResponse;
import com.chuseok22.whoim.server.evidence.application.dto.response.EvidenceDocumentResponse;
import com.chuseok22.whoim.server.evidence.core.constant.DocumentType;
import com.chuseok22.whoim.server.evidence.infrastructure.entity.EvidenceDocument;
import com.chuseok22.whoim.server.evidence.infrastructure.properties.EvidenceProperties;
import com.chuseok22.whoim.server.evidence.infrastructure.repository.EvidenceDocumentRepository;
import com.chuseok22.whoim.server.gap.infrastructure.repository.GapRepository;
import com.chuseok22.whoim.server.handover.core.constant.HandoverStatus;
import com.chuseok22.whoim.server.handover.infrastructure.entity.HandoverPackage;
import com.chuseok22.whoim.server.handover.infrastructure.repository.HandoverPackageRepository;
import com.chuseok22.whoim.server.member.core.constant.Position;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EvidenceDocumentService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("md", "txt");
  private static final char BYTE_ORDER_MARK = '\uFEFF';

  private final HandoverPackageRepository handoverPackageRepository;
  private final EvidenceDocumentRepository evidenceDocumentRepository;
  private final GapRepository gapRepository;
  private final EvidenceProperties evidenceProperties;

  // 검증 순서: 권한 -> 소유 -> Package 상태 -> 파일
  @Transactional
  public EvidenceDocumentResponse upload(
      AuthenticatedMember requester, UUID handoverPackageId, DocumentType documentType, MultipartFile file) {
    HandoverPackage handoverPackage = findPackage(handoverPackageId);
    if (Position.PREDECESSOR != requester.position() || !isOwner(requester, handoverPackage)) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
    if (handoverPackage.getStatus() == HandoverStatus.PUBLISHED || gapRepository.existsByHandoverPackage(handoverPackage)) {
      throw new CustomException(ErrorCode.STATE_CONFLICT);
    }

    String fileName = extractFileName(file);
    validateExtension(fileName);
    String content = readContent(file);

    EvidenceDocument document = evidenceDocumentRepository.save(EvidenceDocument.builder()
        .handoverPackage(handoverPackage)
        .fileName(fileName)
        .documentType(documentType)
        .content(content)
        .uploadedBy(handoverPackage.getPredecessor())
        .build());
    return EvidenceDocumentResponse.from(document);
  }

  @Transactional(readOnly = true)
  public List<EvidenceDocumentResponse> list(AuthenticatedMember requester, UUID handoverPackageId) {
    HandoverPackage handoverPackage = findPackage(handoverPackageId);
    validateReadable(requester, handoverPackage);
    return evidenceDocumentRepository.findAllByHandoverPackageOrderByCreatedAtAsc(handoverPackage).stream()
        .map(EvidenceDocumentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public EvidenceDocumentDetailResponse get(AuthenticatedMember requester, UUID evidenceDocumentId) {
    EvidenceDocument document = evidenceDocumentRepository.findById(evidenceDocumentId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
    validateReadable(requester, document.getHandoverPackage());
    return EvidenceDocumentDetailResponse.from(document);
  }

  // #24 DLP 적용 지점: AI에 보내는 문서 텍스트는 반드시 이 메서드를 거친다
  public String toAiInputText(EvidenceDocument document) {
    return document.getContent();
  }

  private HandoverPackage findPackage(UUID handoverPackageId) {
    return handoverPackageRepository.findById(handoverPackageId)
        .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private boolean isOwner(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    return handoverPackage.getPredecessor().getId().equals(requester.memberId());
  }

  private void validateReadable(AuthenticatedMember requester, HandoverPackage handoverPackage) {
    boolean reviewer = Position.REVIEWER == requester.position();
    boolean owningPredecessor = Position.PREDECESSOR == requester.position() && isOwner(requester, handoverPackage);
    if (!reviewer && !owningPredecessor) {
      throw new CustomException(ErrorCode.ACCESS_DENIED);
    }
  }

  // 경로가 섞인 이름(../../x.txt, C:\dir\x.txt)은 마지막 구분자 뒤만 쓴다
  private String extractFileName(MultipartFile file) {
    String originalFileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
    int separatorIndex = Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\'));
    return originalFileName.substring(separatorIndex + 1);
  }

  private void validateExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex <= 0) {
      throw new CustomException(ErrorCode.EVIDENCE_EXTENSION_NOT_ALLOWED);
    }
    String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new CustomException(ErrorCode.EVIDENCE_EXTENSION_NOT_ALLOWED);
    }
  }

  private String readContent(MultipartFile file) {
    validateSize(file);
    String decoded = decodeUtf8(readBytes(file));
    String content = normalize(decoded);
    if (content.isBlank()) {
      throw new CustomException(ErrorCode.EVIDENCE_FILE_EMPTY);
    }
    return content;
  }

  private void validateSize(MultipartFile file) {
    if (file.getSize() == 0) {
      throw new CustomException(ErrorCode.EVIDENCE_FILE_EMPTY);
    }
    if (file.getSize() > evidenceProperties.maxFileBytes()) {
      throw new CustomException(ErrorCode.EVIDENCE_FILE_TOO_LARGE);
    }
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new CustomException(ErrorCode.INVALID_REQUEST, e);
    }
  }

  // 깨진 바이트를 대체 문자로 바꾸면 근거 글자 범위가 어긋나므로 엄격하게 거부한다
  private String decodeUtf8(byte[] bytes) {
    String decoded;
    try {
      decoded = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException e) {
      throw new CustomException(ErrorCode.EVIDENCE_NOT_UTF8, e);
    }
    if (decoded.indexOf('\u0000') >= 0) {
      throw new CustomException(ErrorCode.EVIDENCE_NOT_UTF8);
    }
    return decoded;
  }

  // 글자 범위가 저장된 content 기준이므로 BOM 제거와 CRLF 정규화는 저장 전에 끝낸다
  private String normalize(String text) {
    String withoutBom = !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    return withoutBom.replace("\r\n", "\n");
  }
}
