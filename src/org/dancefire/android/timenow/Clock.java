package org.dancefire.android.timenow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.AnalogClock;

public class Clock extends AnalogClock {

	public Clock(Context context) {
		super(context);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		canvas.drawLine(0, 0, 100, 100, paint);
	}

}
