package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.response.B2BAnalyticsSummaryDto;
import com.celiloglu.backend.dto.response.B2BCustomerAnalyticsDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminAnalyticsService {

    B2BAnalyticsSummaryDto getAnalyticsSummary();

    Page<B2BCustomerAnalyticsDto> getCustomerAnalytics(int page, int size);

    List<B2BCustomerAnalyticsDto> getAllCustomerAnalytics();
}
