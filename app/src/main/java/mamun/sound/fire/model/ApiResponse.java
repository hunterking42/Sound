package mamun.sound.fire.model;

/** Server envelope: { success, message, data } */
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
}
