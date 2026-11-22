public class DatabaseTest {
    public static void main(String[] args) {

        String password = "DemoPass123!";

        boolean adminRegistered =
                DatabaseManager.registerUser("sl_admin_demo", password);

        boolean employeeRegistered =
                DatabaseManager.registerUser("sl_employee_demo", password);

        System.out.println("Admin registration: " + adminRegistered);
        System.out.println("Employee registration: " + employeeRegistered);

        System.out.println("Admin login: " +
                DatabaseManager.authenticateUser("sl_admin_demo", password));

        System.out.println("Employee login: " +
                DatabaseManager.authenticateUser("sl_employee_demo", password));

        System.out.println("Wrong password test: " +
                DatabaseManager.authenticateUser("sl_admin_demo", "WrongPass123"));
    }
}
