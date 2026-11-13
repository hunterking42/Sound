package mamun.sound.fire.model;

/** Request body for login.php and signup.php */
public class Credentials {
    public final String username;
    public final String password;
    public Credentials(String username, String password) { this.username = username; this.password = password; }
}
