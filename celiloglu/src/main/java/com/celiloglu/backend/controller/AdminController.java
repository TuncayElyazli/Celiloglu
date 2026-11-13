package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.ProductRequestDto;
import com.celiloglu.backend.dto.response.AdminDashboardDto;
import com.celiloglu.backend.dto.response.ProductResponseDto;
import com.celiloglu.backend.service.AdminDashboardService;
import com.celiloglu.backend.service.ProductExcelExportService;
import com.celiloglu.backend.service.ProductFormOptionsService;
import com.celiloglu.backend.service.ProductImageStorageService;
import com.celiloglu.backend.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final ProductService productService;
    private final ProductFormOptionsService optionsService;
    private final ProductImageStorageService imageStorageService;
    private final AdminDashboardService dashboardService;
    private final ProductExcelExportService productExcelExportService;

    @Autowired
    public AdminController(ProductService productService,
            ProductFormOptionsService optionsService,
            ProductImageStorageService imageStorageService,
            AdminDashboardService dashboardService,
            ProductExcelExportService productExcelExportService) {
        this.productService = productService;
        this.optionsService = optionsService;
        this.imageStorageService = imageStorageService;
        this.dashboardService = dashboardService;
        this.productExcelExportService = productExcelExportService;
    }

    public AdminController(ProductService productService,
            ProductFormOptionsService optionsService,
            ProductImageStorageService imageStorageService,
            AdminDashboardService dashboardService) {
        this(productService, optionsService, imageStorageService, dashboardService, null);
    }

    @InitBinder("productForm")
    public void bindProductFields(WebDataBinder binder) {
        binder.setAllowedFields("name", "code", "description", "price", "stockQuantity",
                "yearRange", "imageUrl", "engine", "oemCode", "isActive", "categoryId", "carBrandId");
    }

    @GetMapping({ "/admin", "/admin.html" })
    public String admin(@RequestParam(name = "tab", defaultValue = "dashboard") String tab,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        if ("orders".equals(tab)) {
            return "redirect:/admin/orders";
        }
        if ("payments".equals(tab)) {
            return "redirect:/admin/payments";
        }
        if ("analytics".equals(tab) || "b2b-analytics".equals(tab)) {
            return "redirect:/admin/analytics";
        }
        String activeTab = "products".equals(tab) ? "products" : "dashboard";
        return renderAdmin(model, new ProductRequestDto(), null, false, activeTab, page);
    }

    @GetMapping("/admin/products/new")
    public String newProduct(Model model) {
        ProductRequestDto form = new ProductRequestDto();
        form.setIsActive(true);
        return renderAdmin(model, form, null, true, "products", 0);
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editProduct(@PathVariable("id") Long id, Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ProductResponseDto product = productService.getProductById(id);
            return renderAdmin(model, toRequest(product), id, true, "products", 0);
        } catch (EntityNotFoundException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("productError", ex.getMessage());
            return "redirect:/admin?tab=products";
        }
    }

    @GetMapping("/admin/products/export/excel")
    public ResponseEntity<byte[]> exportProductsToExcel() {
        byte[] excelData = productExcelExportService.exportProductsToExcel();
        String filename = "mehsullar_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @PostMapping("/admin/products")
    public String createProduct(@ModelAttribute("productForm") ProductRequestDto form,
            BindingResult errors,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) {
        return saveProduct(null, form, errors, imageFile, model, redirectAttributes);
    }

    @PostMapping("/admin/products/{id}")
    public String updateProduct(@PathVariable("id") Long id,
            @ModelAttribute("productForm") ProductRequestDto form,
            BindingResult errors,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            Model model, RedirectAttributes redirectAttributes) {
        return saveProduct(id, form, errors, imageFile, model, redirectAttributes);
    }

    @PostMapping("/admin/products/{id}/deactivate")
    public String deactivateProduct(@PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productSuccess", "Məhsul deaktiv edildi.");
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("productError", ex.getMessage());
        }
        return "redirect:/admin?tab=products";
    }

    private String saveProduct(Long id, ProductRequestDto form, BindingResult errors,
            MultipartFile imageFile, Model model,
            RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            boolean hasSpecificError = false;
            for (FieldError fieldError : errors.getFieldErrors()) {
                String field = fieldError.getField();
                String message = switch (field) {
                    case "price" -> "Qiymət düzgün rəqəm olmalıdır (məsələn: 12.50).";
                    case "stockQuantity" -> "Stok miqdarı tam ədəd və minimum 0 olmalıdır.";
                    case "categoryId" -> "Düzgün kateqoriya seçilməlidir.";
                    case "carBrandId" -> "Düzgün avtomobil markası seçilməlidir.";
                    case "yearRange" -> "İl cari ildən böyük ola bilməz.";
                    default -> "Daxil edilmiş məlumat düzgün deyil.";
                };
                errors.reject("product.binding." + field, message);
                hasSpecificError = true;
            }
            if (!hasSpecificError) {
                errors.reject("product.binding", "Rəqəm və seçim sahələrini düzgün doldurun.");
            }
            return renderAdmin(model, form, id, true, "products", 0);
        }

        String previousImageUrl = form.getImageUrl();
        String uploadedImageUrl = null;
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                uploadedImageUrl = imageStorageService.store(imageFile);
                form.setImageUrl(uploadedImageUrl);
            } else {
                form.setImageUrl(validateImageUrl(form.getImageUrl()));
            }

            if (id == null) {
                productService.createProduct(form);
            } else {
                productService.updateProduct(id, form);
            }
            redirectAttributes.addFlashAttribute("productSuccess",
                    id == null ? "Məhsul əlavə edildi." : "Məhsul yeniləndi.");
            return "redirect:/admin?tab=products";
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("İl") || msg.contains("ili") || msg.contains("İstehsal ili"))) {
                errors.rejectValue("yearRange", "product.yearRange", msg);
            }
            errors.reject("product.invalid", msg != null ? msg : "Daxil edilmiş məlumat düzgün deyil.");
        } catch (DataIntegrityViolationException ex) {
            log.warn("Məhsul saxlanılarkən baza məhdudiyyəti pozuldu.", ex);
            errors.reject("product.constraint",
                    "Məhsul saxlanılmadı. Kodun təkrarlanmadığını və məlumatların uzunluğunu yoxlayın.");
        } catch (IOException ex) {
            log.warn("Məhsul şəkli saxlanılmadı.", ex);
            errors.reject("product.image", "Şəkil oxunmadı və ya saxlanılmadı. Başqa şəkillə yenidən yoxlayın.");
        } catch (Exception ex) {
            log.error("Məhsul saxlanılarkən gözlənilməz xəta baş verdi.", ex);
            errors.reject("product.error", "Gözlənilməz xəta baş verdi. Zəhmət olmasa, məlumatları yoxlayıb yenidən cəhd edin.");
        }

        imageStorageService.deleteNewUpload(uploadedImageUrl);
        form.setImageUrl(previousImageUrl);
        return renderAdmin(model, form, id, true, "products", 0);
    }

    private String renderAdmin(Model model, ProductRequestDto form, Long editingId,
            boolean openModal, String activeTab, int page) {
        Page<ProductResponseDto> productPage = productService.getAllProducts(
                PageRequest.of(Math.max(page, 0), 20, Sort.by(Sort.Direction.DESC, "id")));
        List<ProductFormOptionsService.Choice> categories = optionsService.getCategories();
        List<ProductFormOptionsService.Choice> carBrands = optionsService.getCarBrands();

        // Xəta olduqda Spring-in form məlumatlarını və BindingResult-u saxlayırıq.
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", form);
        }
        AdminDashboardDto dashboard = (dashboardService != null)
                ? dashboardService.getDashboardData()
                : new AdminDashboardDto();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("recentOrders", dashboard.getRecentOrders());
        model.addAttribute("activePage", "admin");
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categories);
        model.addAttribute("carBrands", carBrands);
        model.addAttribute("selectedCategoryName", selectedName(categories, form.getCategoryId()));
        model.addAttribute("selectedCarBrandName", selectedName(carBrands, form.getCarBrandId()));
        model.addAttribute("editingProductId", editingId);
        model.addAttribute("openProductModal", openModal);
        model.addAttribute("activeAdminTab", activeTab);
        return "admin";
    }

    private String selectedName(List<ProductFormOptionsService.Choice> choices, Long id) {
        if (id == null || choices == null) {
            return "Seçilməyib";
        }
        return choices.stream()
                .filter(choice -> id.equals(choice.getId()))
                .map(ProductFormOptionsService.Choice::getName)
                .findFirst().orElse("Seçilməyib");
    }

    private String validateImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String url = value.trim();
        if (url.length() > 255) {
            throw new IllegalArgumentException("Şəkil linki maksimum 255 simvol ola bilər.");
        }
        if (url.matches("/product-images/[0-9a-f-]{36}\\.(jpg|png)")) {
            return url;
        }
        try {
            URI uri = URI.create(url);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) {
                return url;
            }
        } catch (IllegalArgumentException ignored) {
            // Aşağıdakı mesaj formanın içində göstəriləcək.
        }
        throw new IllegalArgumentException("Şəkil üçün düzgün http/https linki yazın və ya fayl seçin.");
    }

    private ProductRequestDto toRequest(ProductResponseDto product) {
        ProductRequestDto form = new ProductRequestDto();
        form.setName(product.getName());
        form.setCode(product.getCode());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setStockQuantity(product.getStockQuantity());
        form.setYearRange(product.getYearRange());
        form.setImageUrl(product.getImageUrl());
        form.setEngine(product.getEngine());
        form.setOemCode(product.getOemCode());
        form.setIsActive(Boolean.TRUE.equals(product.getIsActive()));
        form.setCategoryId(product.getCategoryId());
        form.setCarBrandId(product.getCarBrandId());
        return form;
    }
}
