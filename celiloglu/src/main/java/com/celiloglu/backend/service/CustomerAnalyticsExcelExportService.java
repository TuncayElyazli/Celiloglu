package com.celiloglu.backend.service;

public interface CustomerAnalyticsExcelExportService {

    /**
     * Bütün müştərilərin hesablaşma məlumatlarını (Müştəri Hesablaşmaları)
     * Apache POI vasitəsilə .xlsx formatında bayt massivinə çıxarır.
     *
     * @return .xlsx faylının bayt massivi
     */
    byte[] exportCustomerAnalyticsToExcel();
}

