package ru.lenindb.edgedetection;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.net.URI;


public class MainActivity extends Activity {

    private ImageView beforeImageView;
    private ImageView afterImageView;

    private static int RESULT_LOAD_IMAGE = 1;
    private Bitmap bitmapBefore;
    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;
    private static final int STRIDE = 64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beforeImageView=(ImageView)findViewById(R.id.beforeImageView);
        afterImageView=(ImageView)findViewById(R.id.afterImageView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void pick_image_click(View view){
        Intent i=new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==RESULT_LOAD_IMAGE && resultCode==RESULT_OK && data!=null){
            Uri selectedImage =data.getData();
//            beforeImageView.setImageBitmap(BitmapFactory.decodeFile(getPath(selectedImage)));
            bitmapBefore=BitmapFactory.decodeFile(getPath(selectedImage));
            beforeImageView.setImageBitmap(bitmapBefore);
            afterImageView.setImageBitmap(detectEdgeOfBitmap(gray(bitmapBefore)));
        }


    }

    public String getPath(Uri uri){
        if( uri == null ) {
            return null;
        }
        String[] filePathColumn={MediaStore.Images.Media.DATA};
        Cursor cursor= getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex=cursor.getColumnIndex(filePathColumn[0]);
        String picturePath=cursor.getString(columnIndex);
        cursor.close();

        return picturePath;
    }

    private Bitmap getEdgeBitmap(Bitmap bitmap){
        int height=bitmap.getHeight();
        int width=bitmap.getWidth();

        int[] mColors = createColors(bitmap);
        int[] colors = mColors;


        Bitmap newBitmap = Bitmap.createBitmap(colors, 0, STRIDE, WIDTH, HEIGHT,
                Bitmap.Config.RGB_565);

        return newBitmap;
    }

    private static int[] createColors(Bitmap bitmap) {
        int[] colors = new int[STRIDE * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int r = x * 255 / (WIDTH - 1);
                int g = y * 255 / (HEIGHT - 1);
                int b = 255 - Math.min(r, g);
                int a = Math.max(r, g);
                colors[y * STRIDE + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return colors;
    }


    private Bitmap detectEdgeOfBitmap(Bitmap bitmap){
        int[] colors = new int[(bitmap.getHeight()+16) * bitmap.getHeight()];
        int[][] sobel_x = new int[][]{{-1,0,1},
                {-2,0,2},
                {-1,0,1}};

        int [][] sobel_y = new int[][]{{-1,-2,-1},
                {0,0,0},
                {1,2,1}};
        for (int y = 1; y < bitmap.getHeight()-1; y++) {
            for (int x = 1; x < bitmap.getWidth()-1; x++) {
                int pixel_x = ((sobel_x[0][0] * bitmap.getPixel(x-1,y-1)) + (sobel_x[0][1] * bitmap.getPixel(x,y-1)) + (sobel_x[0][2] * bitmap.getPixel(x+1,y-1)) +
                        (sobel_x[1][0] * bitmap.getPixel(x-1,y))   + (sobel_x[1][1] * bitmap.getPixel(x,y))   + (sobel_x[1][2] * bitmap.getPixel(x+1,y)) +
                        (sobel_x[2][0] * bitmap.getPixel(x-1,y+1)) + (sobel_x[2][1] * bitmap.getPixel(x,y+1)) + (sobel_x[2][2] * bitmap.getPixel(x+1,y+1)))/3;

                int pixel_y = ((sobel_y[0][0] * bitmap.getPixel(x-1,y-1)) + (sobel_y[0][1] * bitmap.getPixel(x,y-1)) + (sobel_y[0][2] * bitmap.getPixel(x+1,y-1)) +
                        (sobel_y[1][0] * bitmap.getPixel(x-1,y))   + (sobel_y[1][1] * bitmap.getPixel(x,y))   + (sobel_y[1][2] * bitmap.getPixel(x+1,y)) +
                        (sobel_y[2][0] * bitmap.getPixel(x-1,y+1)) + (sobel_y[2][1] * bitmap.getPixel(x,y+1)) + (sobel_y[2][2] * bitmap.getPixel(x+1,y+1)))/3;

                int val=(int) Math.sqrt((pixel_x * pixel_x) + (pixel_y * pixel_y));
                colors[y*(bitmap.getHeight()+16)+x] = val;
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(colors, 0, (bitmap.getHeight()+16), bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.RGB_565);

        return newBitmap;
    }


    private Bitmap gray(Bitmap bitmap){
        int[] colors = new int[(bitmap.getHeight()+16) * bitmap.getHeight()];

        for (int y = 0; y < bitmap.getHeight()-1; y++) {
            for (int x = 0; x < bitmap.getWidth()-1; x++) {

                int pixel=bitmap.getPixel(x,y);
                int redValue = Color.red(pixel);
                int blueValue = Color.blue(pixel);
                int greenValue = Color.green(pixel);

                int val=Math.max(Math.max(redValue,blueValue),greenValue);
                colors[y*(bitmap.getHeight()+16)+x] = val;
            }
        }

        Bitmap newBitmap = Bitmap.createBitmap(colors, 0, (bitmap.getHeight()+16), bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.RGB_565);

        return newBitmap;
    }

}
