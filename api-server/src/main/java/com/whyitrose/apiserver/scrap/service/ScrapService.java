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
import org.springframework.data.domain.Sort;
import com.whyitrose.apiserver.scrap.dto.MyScrapSearchItemResponse;
import com.whyitrose.apiserver.scrap.dto.MyScrapSearchPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private static final int MAX_ACTIVE_SCRAPS = 50;

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
            validateActiveScrapLimit(userId); // 재활성화도 한도 체크
            scrap.reactivate();
            return;
        }

        validateActiveScrapLimit(userId); // 신규 추가 한도 체크
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
        PageRequest pageRequest = PageRequest.of(
                0,
                MAX_ACTIVE_SCRAPS,
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return scrapRepository.findByUserIdAndStatus(userId, Status.ACTIVE, pageRequest)
                .stream()
                .map(ScrapResponse::from)
                .toList();
    }

    private void validateActiveScrapLimit(Long userId) {
        long activeCount = scrapRepository.countByUserIdAndStatus(userId, Status.ACTIVE);
        if (activeCount >= MAX_ACTIVE_SCRAPS) {
            throw new BaseException(ScrapErrorCode.SCRAP_LIMIT_EXCEEDED);
        }
    }

    public MyScrapSearchPageResponse searchMyScraps(Long userId, String q, int page, int size, String sort) {
        String keyword = (q == null) ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Scrap> scrapPage;
        if (keyword.isBlank()) {
            scrapPage = scrapRepository.findByUserIdAndStatus(userId, Status.ACTIVE, pageable);
        } else {
            scrapPage = scrapRepository
                    .findByUserIdAndStatusAndEvent_Stock_NameContainingIgnoreCaseOrUserIdAndStatusAndEvent_Stock_TickerContainingIgnoreCase(
                            userId, Status.ACTIVE, keyword,
                            userId, Status.ACTIVE, keyword,
                            pageable
                    );
        }

        return new MyScrapSearchPageResponse(
                scrapPage.getContent().stream()
                        .map(MyScrapSearchItemResponse::from)
                        .toList(),
                scrapPage.getNumber(),
                scrapPage.getSize(),
                scrapPage.getTotalElements(),
                scrapPage.getTotalPages()
        );
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Order.desc("updatedAt"));
        }

        String[] tokens = sort.split(",");
        String property = tokens[0].trim();
        String direction = (tokens.length > 1) ? tokens[1].trim() : "desc";

        return "asc".equalsIgnoreCase(direction)
                ? Sort.by(Sort.Order.asc(property))
                : Sort.by(Sort.Order.desc(property));
    }

}

