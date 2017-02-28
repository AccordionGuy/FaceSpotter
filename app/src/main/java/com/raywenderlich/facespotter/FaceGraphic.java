package com.raywenderlich.facespotter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.raywenderlich.facespotter.ui.camera.GraphicOverlay;

import static android.graphics.Color.rgb;


class FaceGraphic extends GraphicOverlay.Graphic {
  private static final float EYE_RADIUS_PROPORTION = 0.45f;
  private static final float IRIS_RADIUS_PROPORTION = EYE_RADIUS_PROPORTION / 2.0f;
  private static final float ID_TEXT_SIZE = 60.0f;

  private Context mContext;
  private boolean mIsFrontFacing;

  private Paint mEyeWhitesPaint;
  private Paint mEyeIrisPaint;
  private Paint mEyeOutlinePaint;
  private Paint mEyeLidPaint;
  private Paint mTextPaint;

  // We want each iris to move independently,
  // so each one gets its own physics engine.
  private EyePhysics mLeftPhysics = new EyePhysics();
  private EyePhysics mRightPhysics = new EyePhysics();

  private Drawable mPigNoseGraphic;
  private Drawable mHappyStarGraphic;
  private Drawable mMustacheGraphic;

  private volatile PointF mLeftEyePosition;
  private volatile boolean mLeftEyeOpen;
  private volatile PointF mRightEyePosition;
  private volatile boolean mRightEyeOpen;
  private volatile PointF mNoseBasePosition;
  private volatile PointF mMouthLeftPosition;
  private volatile PointF mBottomMouthPosition;
  private volatile PointF mMouthRightPosition;
  private volatile float mSmileFactor;


  FaceGraphic(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    super(overlay);

    final int POWDER_BLUE_COLOR = Color.rgb(176,224,230);
    final int SADDLE_BROWN_COLOR = rgb(139,69,19);

    mContext = context;
    mIsFrontFacing = isFrontFacing;

    mPigNoseGraphic = mContext.getDrawable(R.drawable.pig_nose_emoji);
    mHappyStarGraphic = mContext.getDrawable(R.drawable.happy_star);
    mMustacheGraphic = mContext.getDrawable(R.drawable.mustache);


    // Define drawing styles
    // =====================

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


  void update(PointF leftEyePosition,
              boolean leftEyeOpen,
              PointF rightEyePosition,
              boolean rightEyeOpen,
              PointF noseBasePosition,
              PointF mouthLeftPosition,
              PointF bottomMouthPosition,
              PointF mouthRightPosition,
              float smileFactor) {
    mLeftEyePosition = leftEyePosition;
    mLeftEyeOpen = leftEyeOpen;

    mRightEyePosition = rightEyePosition;
    mRightEyeOpen = rightEyeOpen;

    mNoseBasePosition = noseBasePosition;

    mMouthLeftPosition = mouthLeftPosition;
    mBottomMouthPosition = bottomMouthPosition;
    mMouthRightPosition = mouthRightPosition;
    mSmileFactor = smileFactor;

    postInvalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    PointF detectLeftPosition = mLeftEyePosition;
    PointF detectRightPosition = mRightEyePosition;
    PointF detectNoseBasePosition = mNoseBasePosition;
    PointF detectMouthLeftPosition = mMouthLeftPosition;
    PointF detectBottomMouthPosition = mBottomMouthPosition;
    PointF detectMouthRightPosition = mMouthRightPosition;
    if ((detectLeftPosition == null) ||
        (detectRightPosition == null) ||
        (detectNoseBasePosition == null) ||
        (detectMouthLeftPosition == null) ||
        (detectBottomMouthPosition == null) ||
        (detectMouthRightPosition == null)) {
      return;
    }

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

    // Use the Pythagorean formula to calculate the distance between the eyes.
    // We’ll use this distance to set the eyes' size.
    float distance = (float) Math.sqrt(
      (rightEyePosition.x - leftEyePosition.x) * (rightEyePosition.x - leftEyePosition.x) +
      (rightEyePosition.y - leftEyePosition.y) * ((rightEyePosition.y - leftEyePosition.y)));

    float eyeRadius = EYE_RADIUS_PROPORTION * distance;
    float irisRadius = IRIS_RADIUS_PROPORTION * distance;

    // Draw the left eye.
    PointF leftIrisPosition = mLeftPhysics.nextIrisPosition(leftEyePosition, eyeRadius, irisRadius);
    drawEye(canvas, leftEyePosition, eyeRadius, leftIrisPosition, irisRadius, mLeftEyeOpen, mSmileFactor);

    // Draw the right eye.
    PointF rightIrisPosition = mRightPhysics.nextIrisPosition(rightEyePosition, eyeRadius, irisRadius);
    drawEye(canvas, rightEyePosition, eyeRadius, rightIrisPosition, irisRadius, mRightEyeOpen, mSmileFactor);

    drawMustache(canvas, noseBasePosition, mouthLeftPosition, mouthRightPosition);
    drawNose(canvas, noseBasePosition, leftEyePosition, rightEyePosition, irisRadius);
  }

  private void drawEye(Canvas canvas, PointF eyePosition, float eyeRadius,
                       PointF irisPosition, float irisRadius, boolean isOpen,
                       float smileFactor) {
    if (isOpen) {
      canvas.drawCircle(eyePosition.x, eyePosition.y, eyeRadius, mEyeWhitesPaint);
      if ( smileFactor < 0.8 ) {
        canvas.drawCircle(irisPosition.x, irisPosition.y, irisRadius, mEyeIrisPaint);
      }
      else {
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
    mMustacheGraphic.setBounds(left, top, right, bottom);
    mMustacheGraphic.draw(canvas);
  }

}