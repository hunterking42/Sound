# Gson reads these classes by field name, so R8 must not rename them.
-keep class mamun.sound.fire.model.** { *; }

# Generic types and annotations that Retrofit and Gson read at runtime
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Retrofit service interfaces (Retrofit's own rules cover most of it; these make the intent explicit)
-keep interface mamun.sound.fire.network.ApiService { *; }
-keep interface mamun.sound.fire.network.AdminApi { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }
