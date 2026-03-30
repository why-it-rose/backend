package com.whyitrose.apiserver.scrap.service;

import com.whyitrose.apiserver.scrap.dto.ScrapResponse;
import com.whyitrose.apiserver.scrap.exception.ScrapErrorCode;
import com.whyitrose.core.exception.BaseException;
import com.whyitrose.domain.common.Status;
import com.whyitrose.domain.event.Event;
import com.whyitrose.domain.event.EventRepository;
import com.whyitrose.domain.scrap.Scrap;
import com.whyitrose.domain.scrap.ScrapRepository;
import com.whyitrose.domain.user.User;
import com.whyitrose.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // ── 스크랩 추가 ──────────────────────────────────────────────────────

    @Transactional
    public void addScrap(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ScrapErrorCode.EVENT_NOT_FOUND));

        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getStatus() == Status.ACTIVE)
                .orElseThrow(() -> new BaseException(ScrapErrorCode.EVENT_NOT_FOUND));

        Optional<Scrap> existing = scrapRepository.findByUserIdAndEventId(userId, eventId);

        if (existing.isPresent()) {
            Scrap scrap = existing.get();
            if (scrap.getStatus() == Status.ACTIVE) {
                throw new BaseException(ScrapErrorCode.ALREADY_SCRAPED);
            }
            // 이전에 삭제된 스크랩 재활성화 (unique 제약 유지)
            scrap.reactivate();
            return;
        }

        scrapRepository.save(Scrap.create(user, event));
    }

    // ── 스크랩 취소 ──────────────────────────────────────────────────────

    @Transactional
    public void removeScrap(Long userId, Long eventId) {
        Scrap scrap = scrapRepository.findByUserIdAndEventId(userId, eventId)
                .filter(s -> s.getStatus() == Status.ACTIVE)
                .orElseThrow(() -> new BaseException(ScrapErrorCode.SCRAP_NOT_FOUND));

        scrap.delete();
    }

    // ── 내 스크랩 목록 ────────────────────────────────────────────────────

    public List<ScrapResponse> getMyScraps(Long userId) {
        return scrapRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Status.ACTIVE, PageRequest.of(0, 50))
                .stream()
                .map(ScrapResponse::from)
                .toList();
    }
}
