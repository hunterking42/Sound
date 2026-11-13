package mamun.sound.fire.util;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/** Streams a file the user picked (a content:// Uri) into an upload without loading it all into memory. */
public final class UriRequestBody extends RequestBody {
    private final ContentResolver resolver;
    private final Uri uri;
    private final MediaType type;
    private final long length;

    public UriRequestBody(ContentResolver resolver, Uri uri, @Nullable String mime, long length) {
        this.resolver = resolver;
        this.uri = uri;
        this.type = MediaType.parse(mime != null ? mime : "application/octet-stream");
        this.length = length;
    }

    @Override public MediaType contentType() { return type; }
    @Override public long contentLength() { return length; }      // -1 if unknown

    @Override public void writeTo(BufferedSink sink) throws IOException {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open the file");
            sink.writeAll(Okio.source(in));
        }
    }
}
