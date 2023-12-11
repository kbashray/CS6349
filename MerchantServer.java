import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerchantServer {
    private static final List<Product> productList = new ArrayList<>();
    private static final Map<String, Integer> productInventory = new HashMap<>();

    static {
        // Initialize with some dummy products and their inventory
        productList.add(new Product("P001", "Product 1 Description"));
        productInventory.put("P001", 10); // 10 units of Product 1

        productList.add(new Product("P002", "Product 2 Description"));
        productInventory.put("P002", 5); // 5 units of Product 2

        // ... add more products as needed
    }

    public static List<Product> getProductList() {
        return new ArrayList<>(productList); // Return a copy of the product list
    }

    public static boolean processPurchase(String productId) {
        // Check if the product is available in inventory
        Integer inventoryCount = productInventory.get(productId);
        if (inventoryCount != null && inventoryCount > 0) {
            // Update inventory
            productInventory.put(productId, inventoryCount - 1);
            return true; // Purchase successful
        }
        return false; // Purchase failed (product not available)
    }

    public static void addProduct(Product product) {
        productList.add(product);
        productInventory.put(product.getProductId(), 0); // Initially, inventory is 0
    }

    public static void removeProduct(String productId) {
        productList.removeIf(product -> product.getProductId().equals(productId));
        productInventory.remove(productId);
    }

    public static void updateInventory(String productId, int quantity) {
        productInventory.put(productId, quantity);
    }
}
