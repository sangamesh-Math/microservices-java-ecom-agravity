package com.ecommerce.cart.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String imageUrl;
    private BigDecimal subTotal;

    public CartItem() {
    }

    public CartItem(String productId, String productName, BigDecimal unitPrice, Integer quantity, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.subTotal = calculateSubTotal();
    }

    public BigDecimal calculateSubTotal() {
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.subTotal = calculateSubTotal();
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.subTotal = calculateSubTotal();
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }
}
