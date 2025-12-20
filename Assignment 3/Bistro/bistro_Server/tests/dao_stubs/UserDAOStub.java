package dao_stubs;

import database.UserDAO;
import entities.User;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UserDAOStub extends UserDAO {

    private final Map<String, User> byUserId = new HashMap<>();

    public void putUser(User user) {
        byUserId.put(user.getUserID(), user);
    }

    @Override
    public User getUserByUserID(String userID) throws SQLException {
        return byUserId.get(userID);
    }
}
