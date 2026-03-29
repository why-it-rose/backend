package com.whyitrose.apiserver.memo.service;

import com.whyitrose.apiserver.memo.dto.MemoCreateRequest;
import com.whyitrose.apiserver.memo.dto.MemoResponse;
import com.whyitrose.apiserver.memo.dto.MemoUpdateRequest;
import com.whyitrose.apiserver.memo.exception.MemoErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.event.EventRepository;
import com.whyitrose.domain.memo.Memo;
import com.whyitrose.domain.memo.MemoRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // ── 메모 목록 조회 ───────────────────────────────────────────────────

    public List<MemoResponse> getMemos(Long userId, Long eventId) {
        return memoRepository.findByUserIdAndEventIdAndStatusOrderByCreatedAtDesc(userId, eventId, Status.ACTIVE)
                .stream()
                .map(MemoResponse::from)
                .toList();
    }

    // ── 메모 작성 ────────────────────────────────────────────────────────

    @Transactional
    public MemoResponse createMemo(Long userId, Long eventId, MemoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(MemoErrorCode.MEMO_FORBIDDEN));

        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getStatus() == Status.ACTIVE)
                .orElseThrow(() -> new BaseException(MemoErrorCode.EVENT_NOT_FOUND));

        Memo memo = Memo.create(user, event, request.content());
        memoRepository.save(memo);

        return MemoResponse.from(memo);
    }

    // ── 메모 수정 ────────────────────────────────────────────────────────

    @Transactional
    public MemoResponse updateMemo(Long userId, Long memoId, MemoUpdateRequest request) {
        Memo memo = memoRepository.findByIdAndUserIdAndStatus(memoId, userId, Status.ACTIVE)
                .orElseThrow(() -> new BaseException(MemoErrorCode.MEMO_NOT_FOUND));

        memo.updateContent(request.content());

        return MemoResponse.from(memo);
    }

    // ── 메모 삭제 ────────────────────────────────────────────────────────

    @Transactional
    public void deleteMemo(Long userId, Long memoId) {
        Memo memo = memoRepository.findByIdAndUserIdAndStatus(memoId, userId, Status.ACTIVE)
                .orElseThrow(() -> new BaseException(MemoErrorCode.MEMO_NOT_FOUND));

        memo.delete();
    }
}
