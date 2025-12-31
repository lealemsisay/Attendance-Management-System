package src.attendance;

public class ResourceTest {
    public static void main(String[] args) {
        System.out.println("Testing resource paths:");
        
        // Test FXML path
        testPath("view/login.fxml");
        testPath("/attendance/view/login.fxml");
        testPath("/view/login.fxml");
    }
    
    private static void testPath(String path) {
        try {
            java.net.URL url = ResourceTest.class.getResource(path);
            System.out.println("Path: " + path + " -> " + 
                (url != null ? "FOUND: " + url : "NOT FOUND"));
        } catch (Exception e) {
            System.out.println("Path: " + path + " -> ERROR: " + e.getMessage());
        }
    }
} 