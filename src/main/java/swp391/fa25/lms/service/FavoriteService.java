package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.model.Favorite;
import swp391.fa25.lms.model.Tool;
import swp391.fa25.lms.repository.AccountRepository;
import swp391.fa25.lms.repository.FavoriteRepository;
import swp391.fa25.lms.repository.FeedbackRepository;
import swp391.fa25.lms.repository.ToolRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private FeedbackRepository feedbackRepo;


    /**
     * Lấy Set tool IDs yêu thích của account
     */
    public Set<Long> getFavoriteToolIds(Account account) {
        return favoriteRepository.findByAccount(account).stream()
                .map(fav -> fav.getTool().getToolId())
                .collect(Collectors.toSet());
    }


}
