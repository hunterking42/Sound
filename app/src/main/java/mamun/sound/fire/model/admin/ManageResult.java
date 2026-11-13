package mamun.sound.fire.model.admin;

/** Result of add / edit / delete on sounds, categories and banners. Only the matching field is set. */
public class ManageResult {
    public AdminSound sound;
    public AdminCategory category;
    public AdminBanner banner;
    public Long deleted;
}
