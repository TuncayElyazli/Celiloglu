package com.celiloglu.backend.service;

import java.time.LocalDate;

public interface PaymentExcelExportService {

    /**
     * Seçilmiş tarix aralığına uyğun bütün kassa və bank qəbzlərini
     * Apache POI vasitəsilə .xlsx formatında bayt massivinə çıxarır.
     *
     * @param startDate Başlanğıc tarixi (daxildir, null ola bilər)
     * @param endDate Bitiş tarixi (daxildir, null ola bilər)
     * @return .xlsx faylının bayt massivi
     */
    byte[] exportPaymentsToExcel(LocalDate startDate, LocalDate endDate);
}

