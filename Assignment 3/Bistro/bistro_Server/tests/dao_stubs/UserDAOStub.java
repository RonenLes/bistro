package dao_stubs;

import database.UserDAO;
import entities.User;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UserDAOStub extends UserDAO {

    private final Map<String, User> byUserId = new HashMap<>();
    private boolean throwOnGet = false;

    public void putUser(User user) {
        byUserId.put(user.getUserID(), user);
    }

    /** Simulate SQLException from getUserByUserID (used by tests). */
    public void throwOnGetUserById(boolean value) {
        this.throwOnGet = value;
    }

    @Override
    public User getUserByUserID(String userID) throws SQLException {
        if (throwOnGet) throw new SQLException("Forced SQLException from UserDAOStub");
        return byUserId.get(userID);
    }
}
