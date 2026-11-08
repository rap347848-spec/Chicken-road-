package demo.offline.visual;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.InputStream;

public class MainActivity extends Activity {
    private ImageView image;
    private Overlay overlay;
    private static final int PICK_IMAGE = 41;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,16,16,16);
        root.setBackgroundColor(Color.rgb(245,245,245));

        TextView title = new TextView(this);
        title.setText("محلل بصري تجريبي — صور محلية فقط");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("اختر صورة اختبار محفوظة على جهازك، ثم حرّك الإطارات الثلاثة لتحديد مناطق الصورة. لا يلتقط التطبيق الشاشة ولا يتصل بأي لعبة.");
        note.setTextSize(14);
        note.setTextColor(Color.DKGRAY);
        root.addView(note);

        Button choose = new Button(this);
        choose.setText("اختيار صورة من الجهاز");
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_IMAGE);
        });
        root.addView(choose);

        FrameLayout stage = new FrameLayout(this);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(Color.WHITE);
        stage.addView(image, new FrameLayout.LayoutParams(-1,-1));
        overlay = new Overlay();
        stage.addView(overlay, new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,0,1);
        lp.topMargin = 12;
        root.addView(stage, lp);

        TextView legend = new TextView(this);
        legend.setText("أحمر: منطقة الطريق  |  أزرق: منطقة العنصر  |  أبيض: منطقة الزر التجريبية\nاسحب الإطار لتحريكه، واسحب مقبضه السفلي الأيمن لتغيير حجمه.");
        legend.setTextSize(14);
        legend.setTextColor(Color.DKGRAY);
        root.addView(legend);

        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                image.setImageBitmap(bmp);
                overlay.invalidate();
            } catch (Exception e) {
                Toast.makeText(this, "تعذر فتح الصورة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class Overlay extends View {
        private final Paint p = new Paint(3);
        private final RectF[] boxes = {
            new RectF(.08f,.12f,.92f,.48f),
            new RectF(.35f,.48f,.65f,.72f),
            new RectF(.30f,.78f,.70f,.94f)
        };
        private final int[] colors = {Color.RED, Color.BLUE, Color.WHITE};
        private int selected = -1;
        private float lastX,lastY;
        private boolean resizing;

        Overlay() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float sx=getWidth(), sy=getHeight();
            for (int i=0;i<3;i++) {
                RectF r = new RectF(boxes[i].left*sx, boxes[i].top*sy,
                                    boxes[i].right*sx, boxes[i].bottom*sy);
                p.setColor(colors[i]); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);
                c.drawRect(r,p);
                p.setStyle(Paint.Style.FILL); p.setAlpha(35); c.drawRect(r,p); p.setAlpha(255);
                p.setStyle(Paint.Style.FILL); p.setTextSize(30);
                c.drawText(new String[]{"الطريق","العنصر","الزر"}[i],r.left+8,Math.max(32,r.top+32),p);
                c.drawCircle(r.right,r.bottom,13,p);
            }
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            float x=e.getX()/Math.max(1,getWidth()), y=e.getY()/Math.max(1,getHeight());
            if (e.getAction()==MotionEvent.ACTION_DOWN) {
                selected=-1; resizing=false;
                for(int i=2;i>=0;i--) {
                    RectF r=boxes[i];
                    if(x>=r.left && x<=r.right && y>=r.top && y<=r.bottom) {
                        selected=i; resizing=(Math.abs(x-r.right)<.06f && Math.abs(y-r.bottom)<.06f); break;
                    }
                }
                lastX=x; lastY=y; return selected>=0;
            } else if(e.getAction()==MotionEvent.ACTION_MOVE && selected>=0) {
                RectF r=boxes[selected]; float dx=x-lastX, dy=y-lastY;
                if(resizing) { r.right=clamp(r.right+dx,.08f,1); r.bottom=clamp(r.bottom+dy,.08f,1); }
                else {
                    float w=r.width(),h=r.height();
                    float nl=clamp(r.left+dx,0,1-w), nt=clamp(r.top+dy,0,1-h);
                    r.set(nl,nt,nl+w,nt+h);
                }
                lastX=x; lastY=y; invalidate(); return true;
            } else if(e.getAction()==MotionEvent.ACTION_UP) { selected=-1; return true; }
            return true;
        }
        private float clamp(float v,float lo,float hi){ return Math.max(lo,Math.min(hi,v)); }
    }
}
