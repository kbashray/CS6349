class Product {
    private String productId;
    private String description;

    public Product(String productId, String description) {
        this.productId = productId;
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return productId + ": " + description;
    }
}
