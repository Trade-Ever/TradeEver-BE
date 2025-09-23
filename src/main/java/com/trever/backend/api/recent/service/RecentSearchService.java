package com.trever.backend.api.recent.service;

import com.trever.backend.api.recent.entity.RecentSearch;
import com.trever.backend.api.recent.repository.RecentSearchRepository;
import com.trever.backend.api.user.entity.User;
import com.trever.backend.api.user.repository.UserRepository;
import com.trever.backend.common.exception.NotFoundException;
import com.trever.backend.common.response.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentSearchService {

    private final RecentSearchRepository recentSearchRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addSearch(Long userId, String keyword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOT_FOUND.getMessage()));

        // 같은 키워드 있으면 삭제 (쿼리 메소드 사용)
        recentSearchRepository.deleteByUserIdAndKeyword(userId, keyword);
        recentSearchRepository.flush();

        // 새 검색어 저장
        RecentSearch search = RecentSearch.builder()
                .user(user)
                .keyword(keyword)
                .build();

        recentSearchRepository.save(search);

        // 5개 넘으면 오래된 것 삭제
        List<RecentSearch> searches = recentSearchRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (searches.size() > 5) {
            // 오래된 검색어를 삭제
            searches.subList(5, searches.size())
                    .forEach(recentSearchRepository::delete);
            recentSearchRepository.flush();
        }
    }

    // 최근 검색어 조회
    public List<String> getRecentSearches(Long userId) {
        return recentSearchRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(RecentSearch::getKeyword)
                .distinct() // 혹시라도 DB에 중복 들어간 경우 방지
                .limit(5)
                .toList();
    }

    @Transactional
    public void removeSearch(Long userId, String keyword) {
        recentSearchRepository.deleteByUserIdAndKeyword(userId, keyword);
    }
}
