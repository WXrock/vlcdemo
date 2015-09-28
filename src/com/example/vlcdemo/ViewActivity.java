package com.example.vlcdemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ViewActivity extends Activity {

	private Gallery gallery = null;
	private ImageAdapter adapter = null;
	private String[] paths = null;
	private Bitmap[] images;
	private TextView image_info;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_layout);
		
		this.paths = getIntent().getStringArrayExtra("fileName");
		this.images = new Bitmap[8];
		for(int i=0;i<8;i++){
			images[i] = BitmapFactory.decodeFile(paths[i]);
		}
		this.image_info = (TextView) findViewById(R.id.text_image);
		
		gallery = (Gallery) findViewById(R.id.gallery);
		adapter = new ImageAdapter(this);
		gallery.setAdapter(adapter);
		gallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				ImageView image = (ImageView) findViewById(R.id.imageView);
				image.setImageBitmap(images[arg2]);
				image.setAdjustViewBounds(true);
				image.setMaxHeight(640);
				//image.setMaxWidth(480);
				ViewActivity.this.image_info.setText(paths[arg2]);
			}
		});
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	
	public class ImageAdapter extends BaseAdapter {
		
		Context  context;
		int itemBackfround;
		
		public ImageAdapter(Context c) {
			context = c;
			TypedArray a = obtainStyledAttributes(R.styleable.Gallery1);
			itemBackfround = a.getResourceId(R.styleable.Gallery1_android_galleryItemBackground, 0);
			a.recycle();
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return paths.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			Bitmap bm;
			if(convertView==null) {
				imageView = new ImageView(context);
				imageView.setImageBitmap(images[position]);
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setLayoutParams(new Gallery.LayoutParams(150, 120));
			}else {
				imageView = (ImageView) convertView;
			}
			imageView.setBackgroundResource(itemBackfround);
			
			return imageView;
		}
		
		
	}
	
}
