package com.marketplace.service;

import com.marketplace.dto.ProductResponse;
import com.marketplace.entity.Product;
import com.marketplace.entity.ProductAsset;
import com.marketplace.exception.ApiException;
import com.marketplace.repository.ProductAssetRepository;
import com.marketplace.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAssetRepository productAssetRepository;
    private final String uploadDir;
    private final String storageBaseUrl;

    public ProductService(ProductRepository productRepository,
                           ProductAssetRepository productAssetRepository,
                           @org.springframework.beans.factory.annotation.Value("${app.storage.upload-dir}") String uploadDir,
                           @Value("${app.storage.base-url}") String storageBaseUrl) {
        this.productRepository = productRepository;
        this.productAssetRepository = productAssetRepository;
        this.uploadDir = uploadDir;
        this.storageBaseUrl = storageBaseUrl;
    }

    public List<ProductResponse> listPublished(String countryCode) {
        boolean isIndia = "IN".equalsIgnoreCase(countryCode);
        return productRepository.findByStatus("PUBLISHED").stream()
                .map(p -> toResponse(p, isIndia))
                .toList();
    }

    public ProductResponse getOne(Long id, String countryCode) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Product not found"));
        return toResponse(p, "IN".equalsIgnoreCase(countryCode));
    }

    /** Admin-only: upload a new digital card template (any format accepted, e.g. Canva export). */
    public Product uploadNewProduct(String title, String description, String theme,
                                     int priceInrPaise, int priceUsdCents,
                                     Long adminUserId, MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + "." + ext;
            Path dest = Path.of(uploadDir, "templates", storedName);
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);

            Product product = new Product();
            product.setSku("CARD-" + System.currentTimeMillis());
            product.setTitle(title);
            product.setDescription(description);
            product.setTheme(theme);
            product.setVersion("v1.0");
            product.setPriceInrPaise(priceInrPaise);
            product.setPriceUsdCents(priceUsdCents);
            product.setStatus("PUBLISHED");
            product.setCreatedBy(adminUserId);
            productRepository.save(product);

            ProductAsset asset = new ProductAsset();
            asset.setProduct(product);
            asset.setFileUrl(dest.toString());
            asset.setFileFormat(ext);
            asset.setTemplate(true);
            productAssetRepository.save(asset);

            product.getAssets().add(asset);
            return product;
        } catch (Exception e) {
            throw new ApiException(500, "Failed to upload product: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private ProductResponse toResponse(Product p, boolean isIndia) {
        String previewUrl = p.getAssets().isEmpty() ? null : storageBaseUrl + "/preview/" + p.getId();
        return new ProductResponse(
                p.getId(), p.getSku(), p.getTitle(), p.getDescription(), p.getTheme(), p.getVersion(),
                isIndia ? p.getPriceInrPaise() : p.getPriceUsdCents(),
                isIndia ? "INR" : "USD",
                previewUrl
        );
    }
}
