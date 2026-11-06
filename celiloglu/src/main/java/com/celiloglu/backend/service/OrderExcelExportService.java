package com.celiloglu.backend.service;

public interface OrderExcelExportService {

    /**
     * Sifarişləri və onların məhsullarını iki ayrı vərəqdə ("Sifarişlər" və "Məhsullar")
     * Apache POI vasitəsilə .xlsx formatında bayt massivinə çıxarır.
     *
     * @param includeFinancials Maliyyə məlumatlarının (qiymət, məbləğ, ödəniş, borc) daxil edilib-edilməməsi
     * @return .xlsx faylının bayt massivi
     */
    byte[] exportOrdersToExcel(boolean includeFinancials);
}

