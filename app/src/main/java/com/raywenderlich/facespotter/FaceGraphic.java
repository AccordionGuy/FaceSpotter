package com.raywenderlich.facespotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import static android.graphics.Color.rgb;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.PointF;

import com.raywenderlich.facespotter.ui.camera.GraphicOverlay;


class FaceGraphic extends GraphicOverlay.Graphic {

  private static final String TAG = "FaceGraphic";

  private static final float EYE_RADIUS_PROPORTION = 0.45f;
  private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
  private static final float HEAD_TILT_HAT_THRESHOLD = 20.0f;
  private static final float ID_TEXT_SIZE = 60.0f;

  private Context mContext;
  private boolean mIsFrontFacing;

  private Drawable mPigNoseGraphic;
  private Drawable mHappyStarGraphic;
  private Drawable mMustacheGraphic;
  private Drawable mHatGraphic;

  private Paint mEyeWhitesPaint;
  private Paint mEyeIrisPaint;
  private Paint mEyeOutlinePaint;
  private Paint mEyeLidPaint;
  private Paint mTextPaint;

  // Face coordinate and dimension data
  private volatile PointF mPosition;
  private volatile float mWidth;
  private volatile float mHeight;
  private volatile float mEulerY;
  private volatile float mEulerZ;
  private volatile PointF mLeftEyePosition;
  private volatile boolean mLeftEyeOpen;
  private volatile PointF mRightEyePosition;
  private volatile boolean mRightEyeOpen;
  private volatile PointF mNoseBasePosition;
  private volatile PointF mMouthLeftPosition;
  private volatile PointF mMouthBottomPosition;
  private volatile PointF mMouthRightPosition;
  private volatile boolean mIsSmiling;

  // We want each iris to move independently,
  // so each one gets its own physics engine.
  private EyePhysics mLeftPhysics = new EyePhysics();
  private EyePhysics mRightPhysics = new EyePhysics();


  FaceGraphic(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    super(overlay);

    final int POWDER_BLUE_COLOR = Color.rgb(176,224,230);
    final int SADDLE_BROWN_COLOR = rgb(139,69,19);
    final float TEXT_SIZE = 60.0f;

    mContext = context;
    mIsFrontFacing = isFrontFacing;

    mPigNoseGraphic = mContext.getDrawable(R.drawable.pig_nose_emoji);
    mHappyStarGraphic = mContext.getDrawable(R.drawable.happy_star);
    mMustacheGraphic = mContext.getDrawable(R.drawable.mustache);
    mHatGraphic = mContext.getDrawable(R.drawable.red_hat);

    mEyeWhitesPaint = new Paint();
    mEyeWhitesPaint.setColor(Color.WHITE);
    mEyeWhitesPaint.setStyle(Paint.Style.FILL);

    mEyeLidPaint = new Paint();
    mEyeLidPaint.setColor(POWDER_BLUE_COLOR);
    mEyeLidPaint.setStyle(Paint.Style.FILL);

    mEyeIrisPaint = new Paint();
    mEyeIrisPaint.setColor(SADDLE_BROWN_COLOR);
    mEyeIrisPaint.setStyle(Paint.Style.FILL);

    mEyeOutlinePaint = new Paint();
    mEyeOutlinePaint.setColor(Color.BLACK);
    mEyeOutlinePaint.setStyle(Paint.Style.STROKE);
    mEyeOutlinePaint.setStrokeWidth(5);

    mTextPaint = new Paint();
    mTextPaint.setColor(Color.CYAN);
    mTextPaint.setTextSize(ID_TEXT_SIZE);
  }

  void update(FaceData faceData) {
    mPosition = faceData.getPosition();
    mHeight = faceData.getHeight();
    mWidth = faceData.getWidth();

    mEulerY = faceData.getEulerY();
    mEulerZ = faceData.getEulerZ();

    mLeftEyePosition = faceData.getLeftEyePosition();
    mLeftEyeOpen = faceData.isLeftEyeOpen();
    mRightEyePosition = faceData.getRightEyePosition();
    mRightEyeOpen = faceData.isRightEyeOpen();

    mNoseBasePosition = faceData.getNoseBasePosition();

    mMouthLeftPosition = faceData.getMouthLeftPosition();
    mMouthBottomPosition = faceData.getMouthBottomPosition();
    mMouthRightPosition = faceData.getMouthRightPosition();
    mIsSmiling = faceData.isSmiling();

    postInvalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    // Confirm that the face and its features are still visible
    // before drawing any graphics over it.
    PointF detectPosition = mPosition;
    PointF detectLeftPosition = mLeftEyePosition;
    PointF detectRightPosition = mRightEyePosition;
    PointF detectNoseBasePosition = mNoseBasePosition;
    PointF detectMouthLeftPosition = mMouthLeftPosition;
    PointF detectBottomMouthPosition = mMouthBottomPosition;
    PointF detectMouthRightPosition = mMouthRightPosition;
    if ((detectPosition == null) ||
        (detectLeftPosition == null) ||
        (detectRightPosition == null) ||
        (detectNoseBasePosition == null) ||
        (detectMouthLeftPosition == null) ||
        (detectBottomMouthPosition == null) ||
        (detectMouthRightPosition == null)) {
      return;
    }

    // Convert the face's camera coordinates and dimensions
    // to view coordinates and dimensions.
    PointF position = new PointF(scaleX(detectPosition.x),
                                 scaleY(detectPosition.y));
    float width = scaleX(mWidth);
    float height = scaleY(mHeight);
    PointF leftEyePosition = new PointF(translateX(detectLeftPosition.x),
                                        translateY(detectLeftPosition.y));
    PointF rightEyePosition = new PointF(translateX(detectRightPosition.x),
                                         translateY(detectRightPosition.y));
    PointF noseBasePosition = new PointF(translateX(detectNoseBasePosition.x),
                                         translateY(detectNoseBasePosition.y));
    PointF mouthLeftPosition = new PointF(translateX(detectMouthLeftPosition.x),
                                          translateY(detectMouthLeftPosition.y));
    PointF mouthRightPosition = new PointF(translateX(detectMouthRightPosition.x),
                                           translateY(detectMouthRightPosition.y));

    // Calculate the distance between the eyes using Pythagoras' formula,
    // and we'll use that distance to set the size of the eyes and irises.
    float distance = (float) Math.sqrt(
      (rightEyePosition.x - leftEyePosition.x) * (rightEyePosition.x - leftEyePosition.x) +
      (rightEyePosition.y - leftEyePosition.y) * ((rightEyePosition.y - leftEyePosition.y)));
    float eyeRadius = EYE_RADIUS_PROPORTION * distance;
    float irisRadius = IRIS_RADIUS_PROPORTION * distance;

    // Draw the eyes.
    PointF leftIrisPosition = mLeftPhysics.nextIrisPosition(leftEyePosition, eyeRadius, irisRadius);
    drawEye(canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, mLeftEyeOpen, mIsSmiling);
    PointF rightIrisPosition = mRightPhysics.nextIrisPosition(rightEyePosition, eyeRadius, irisRadius);
    drawEye(canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, mRightEyeOpen, mIsSmiling);

    // Draw the mustache and nose.
    drawMustache(canvas, noseBasePosition, mouthLeftPosition, mouthRightPosition);
    drawNose(canvas, noseBasePosition, leftEyePosition, rightEyePosition, irisRadius);

    // Draw the hat only if the subject's head is titled at a
    // sufficiently jaunty angle.
    if (Math.abs(mEulerZ) > HEAD_TILT_HAT_THRESHOLD) {
      drawHat(canvas, position, width, height, noseBasePosition);
    }
  }

  private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius,
                       PointF irisPosition, float irisRadius, boolean isOpen,
                       boolean isSmiling) {
    if (isOpen) {
      canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitesPaint);
      if ( !isSmiling ) {
        canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mEyeIrisPaint);
      } else {
        mHappyStarGraphic.setBounds((int)(irisPosition.x - irisRadius),
                                    (int)(irisPosition.y - irisRadius),
                                    (int)(irisPosition.x + irisRadius),
                                    (int)(irisPosition.y + irisRadius));
        mHappyStarGraphic.draw(canvas);
      }
    } else {
      canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeLidPaint);
      float y = eyePosition.y;
      float start = eyePosition.x - eyeRadius;
      float end = eyePosition.x + eyeRadius;
      canvas.drawLine(start, y, end, y, mEyeOutlinePaint);
    }
    canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeOutlinePaint);
  }

  private void drawNose(Canvas canvas, PointF noseBasePosition, PointF leftEyePosition, PointF rightEyePosition, float noseWidth) {
    final float NOSE_WIDTH_SCALE_FACTOR = 1.4f;

    int left = (int)(noseBasePosition.x - noseWidth * NOSE_WIDTH_SCALE_FACTOR);
    int right = (int)(noseBasePosition.x + noseWidth * NOSE_WIDTH_SCALE_FACTOR);
    int top = (int)(leftEyePosition.y + rightEyePosition.y) / 2;
    int bottom = (int)noseBasePosition.y;
    mPigNoseGraphic.setBounds(left, top, right, bottom);
    mPigNoseGraphic.draw(canvas);
  }

  private void drawMustache(Canvas canvas, PointF noseBasePosition, PointF mouthLeftPosition, PointF mouthRightPosition) {
    int left = (int)mouthLeftPosition.x;
    int top = (int)noseBasePosition.y;
    int right = (int)mouthRightPosition.x;
    int bottom = (int)Math.min(mouthLeftPosition.y, mouthRightPosition.y);
    if (mIsFrontFacing) {
      mMustacheGraphic.setBounds(left, top, right, bottom);
    } else {
      mMustacheGraphic.setBounds(right, top, left, bottom);
    }
    mMustacheGraphic.draw(canvas);
  }

  private void drawHat(Canvas canvas, PointF facePosition, float faceWidth, float faceHeight, PointF noseBasePosition) {
    float hatCenterY = facePosition.y + (faceHeight / 8);
    float hatWidth = faceWidth / 4;
    float hatHeight = faceHeight / 6;

    int left = (int)(noseBasePosition.x - (hatWidth / 2));
    int right = (int)(noseBasePosition.x + (hatWidth / 2));
    int top = (int)(hatCenterY - (hatHeight / 2));
    int bottom = (int)(hatCenterY + (hatHeight / 2));
    mHatGraphic.setBounds(left, top, right, bottom);
    mHatGraphic.draw(canvas);
  }

}