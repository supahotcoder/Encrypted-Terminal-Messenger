package Server;

public class Test {

    // Example Usage of UserDatabase
    // Creating and populating the database
    public static void main(String args[]) {
        UserDatabase database = new UserDatabase("src/Server/data.txt");

        database.addUser("admin", "admin");
        database.addUser("alice", "nbusr123");
        database.addUser("bob", "bob");
        database.addUser("chuck", "chuck123");
        database.addUser("ss", "ss");
    }
}
