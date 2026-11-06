package com.celiloglu.backend.service;

public interface ProductExcelExportService {

    /**
     * Bütün məhsulları (aktiv, deaktiv və stoku 0 olanlar daxil)
     * Apache POI vasitəsilə .xlsx faylı şəklində bayt massivinə çıxarır.
     *
     * @return .xlsx faylının bayt massivi
     */
    byte[] exportProductsToExcel();
}

