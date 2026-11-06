package com.celiloglu.backend.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ProductUploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("productError",
                "Şəkil yüklənmə həddini aşdı. Maksimum 1 MB PNG və ya JPG seçib formanı yenidən doldurun.");
        return "redirect:/admin?tab=products";
    }
}
