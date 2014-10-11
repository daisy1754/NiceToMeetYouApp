package jp.gr.java_conf.daisy.n2mu;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

public class RoundTransformation implements com.squareup.picasso.Transformation {

    @Override
    public Bitmap transform(final Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        BitmapShader shader = new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2.f;
        canvas.drawCircle(r, r, r, paint);

        source.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "rounded";
    }
}