package swp391.fa25.lms.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.SellerSubscription;
import swp391.fa25.lms.repository.SellerSubscriptionRepository;

@Service
@RequiredArgsConstructor
public class SellerSubscriptionService {

    private final SellerSubscriptionRepository repo;

    public Page<SellerSubscription> findForSeller(Long sellerId, Pageable pageable) {
        return repo.findByAccountAccountId(sellerId, pageable);
    }

    public Page<SellerSubscription> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

}
