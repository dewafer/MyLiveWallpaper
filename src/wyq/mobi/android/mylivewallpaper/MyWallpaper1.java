package wyq.mobi.android.mylivewallpaper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;

public class MyWallpaper1 extends GLWallpaperService {

	private SensorManager mSensorManager;

	@Override
	public void onCreate() {
		super.onCreate();
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}

	@Override
	public GLEngine onCreateEngine() {
		return new MyEngine();
	}

	class MyEngine extends GLEngine implements GLWallpaperService.Renderer,
			SensorEventListener {
		private Cube mCube;
		private Sensor mRotationVectorSensor;
		private final float[] mRotationMatrix = new float[16];

		public MyEngine() {
			// find the rotation-vector sensor
			mRotationVectorSensor = mSensorManager
					.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

			mCube = new Cube();
			// initialize the rotation matrix to identity
			mRotationMatrix[0] = 1;
			mRotationMatrix[4] = 1;
			mRotationMatrix[8] = 1;
			mRotationMatrix[12] = 1;

			// set renderer
			setRenderer(this);
			setRenderMode(RENDERMODE_CONTINUOUSLY);

			// By default they are turned off.
			setTouchEventsEnabled(true);
			// It is a performance optimization to set this to false.
			setOffsetNotificationsEnabled(false);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			if (visible) {
				start();
			} else {
				stop();
			}
		}

		public void start() {
			// enable our sensor when the activity is resumed, ask for
			// 10 ms updates.
			mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
		}

		public void stop() {
			// make sure to turn our sensor off when the activity is paused
			mSensorManager.unregisterListener(this);
		}

		public void onSensorChanged(SensorEvent event) {
			// we received a sensor event. it is a good practice to check
			// that we received the proper event
			if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
				// convert the rotation-vector to a 4x4 matrix. the matrix
				// is interpreted by Open GL as the inverse of the
				// rotation-vector, which is what we want.
				SensorManager.getRotationMatrixFromVector(mRotationMatrix,
						event.values);
			}
		}

		public void onDrawFrame(GL10 gl) {
			// clear screen
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

			// set-up modelview matrix
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glTranslatef(0, 0, -3.0f);
			gl.glMultMatrixf(mRotationMatrix, 0);
			// rotate
			gl.glRotatef(mAngleX, 0, 1, 0);
			gl.glRotatef(mAngleY, 1, 0, 0);

			// draw our object
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

			mCube.draw(gl);
		}

		public void onSurfaceChanged(GL10 gl, int width, int height) {
			// set view-port
			gl.glViewport(0, 0, width, height);
			// set projection matrix
			float ratio = (float) width / height;
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
		}

		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			// dither is enabled by default, we don't need it
			gl.glDisable(GL10.GL_DITHER);
			// clear screen in white
			// gl.glClearColor(1, 1, 1, 1);
			// clear screen in black, save power?
			gl.glClearColor(0, 0, 0, 0);

		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				float dx = x - mPreviousX;
				float dy = y - mPreviousY;
				mAngleX += dx * TOUCH_SCALE_FACTOR;
				mAngleY += dy * TOUCH_SCALE_FACTOR;
				requestRender();
			}
			mPreviousX = x;
			mPreviousY = y;
		}

		public float mAngleX;
		public float mAngleY;
		private float mPreviousX;
		private float mPreviousY;
		private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

		class Cube {
			// initialize our cube
			private FloatBuffer mVertexBuffer;
			private FloatBuffer mColorBuffer;
			private ByteBuffer mIndexBuffer;

			public Cube() {
				final float vertices[] = { -1, -1, -1, 1, -1, -1, 1, 1, -1, -1,
						1, -1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, };

				final float colors[] = { 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0,
						1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1,
						1, };

				final byte indices[] = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6, 2, 2,
						6, 7, 2, 7, 3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3,
						0, 1, 3, 1, 2 };

				ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
				vbb.order(ByteOrder.nativeOrder());
				mVertexBuffer = vbb.asFloatBuffer();
				mVertexBuffer.put(vertices);
				mVertexBuffer.position(0);

				ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length * 4);
				cbb.order(ByteOrder.nativeOrder());
				mColorBuffer = cbb.asFloatBuffer();
				mColorBuffer.put(colors);
				mColorBuffer.position(0);

				mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
				mIndexBuffer.put(indices);
				mIndexBuffer.position(0);
			}

			public void draw(GL10 gl) {
				gl.glEnable(GL10.GL_CULL_FACE);
				gl.glFrontFace(GL10.GL_CW);
				gl.glShadeModel(GL10.GL_SMOOTH);
				gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
				gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
				gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE,
						mIndexBuffer);
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// Do nothing.
		}
	}

}
