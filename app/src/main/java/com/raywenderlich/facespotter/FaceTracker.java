package com.raywenderlich.facespotter;

import android.content.Context;
import android.graphics.PointF;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.raywenderlich.facespotter.ui.camera.GraphicOverlay;

import java.util.HashMap;
import java.util.Map;


class FaceTracker extends Tracker<Face> {
  private static final float EYE_CLOSED_THRESHOLD = 0.4f;

  private GraphicOverlay mOverlay;
  private FaceGraphic mFaceGraphic;
  private Context mContext;
  private boolean mIsFrontFacing;

  // As subjects move, part or all of their faces may momentarily move
  // too quickly to detect features or out of the tracker's detection range.
  // We keep a record of previously detected facial landmarks so that
  // we can approximate their locations during these momentary "disappearances".
  private Map<Integer, PointF> mPreviousProportions = new HashMap<>();

  // Similarly, keep track of the previous eye open state so that it can be reused for
  // intermediate frames which lack eye landmarks and corresponding eye state.
  private boolean mPreviousIsLeftOpen = true;
  private boolean mPreviousIsRightOpen = true;


  FaceTracker(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    mOverlay = overlay;
    mContext = context;
    mIsFrontFacing = isFrontFacing;
  }

  // Create a new graphic when a new face is detected.
  @Override
  public void onNewItem(int id, Face face) {
    mFaceGraphic = new FaceGraphic(mOverlay, mContext, mIsFrontFacing);
  }

  // Update the eyes, nose, and mustache based on the most recent face detection results.
  @Override
  public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
    mOverlay.add(mFaceGraphic);
    updatePreviousProportions(face);

    PointF leftEyePosition = getLandmarkPosition(face, Landmark.LEFT_EYE);
    PointF rightEyePosition = getLandmarkPosition(face, Landmark.RIGHT_EYE);
    PointF noseBasePosition = getLandmarkPosition(face, Landmark.NOSE_BASE);
    PointF mouthLeftPosition =  getLandmarkPosition(face, Landmark.LEFT_MOUTH);
    PointF mouthRightPosition = getLandmarkPosition(face, Landmark.RIGHT_MOUTH);
    PointF bottomMouthPosition = getLandmarkPosition(face, Landmark.BOTTOM_MOUTH);

    float leftOpenScore = face.getIsLeftEyeOpenProbability();
    boolean isLeftOpen;
    if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
      isLeftOpen = mPreviousIsLeftOpen;
    } else {
      isLeftOpen = (leftOpenScore > EYE_CLOSED_THRESHOLD);
      mPreviousIsLeftOpen = isLeftOpen;
    }

    float rightOpenScore = face.getIsRightEyeOpenProbability();
    boolean isRightOpen;
    if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
      isRightOpen = mPreviousIsRightOpen;
    } else {
      isRightOpen = (rightOpenScore > EYE_CLOSED_THRESHOLD);
      mPreviousIsRightOpen = isRightOpen;
    }

    float smileRating = face.getIsSmilingProbability();

    mFaceGraphic.update(
      leftEyePosition,
      isLeftOpen,
      rightEyePosition,
      isRightOpen,
      noseBasePosition,
      mouthLeftPosition,
      bottomMouthPosition,
      mouthRightPosition,
      smileRating);
  }

  // This method is called when a subject's face momentarily
  // goes undetected.
  @Override
  public void onMissing(FaceDetector.Detections<Face> detectionResults) {
    mOverlay.remove(mFaceGraphic);
  }

  // This method is called when a subject's face is assumed
  // to be out of camera view for good.
  @Override
  public void onDone() {
    mOverlay.remove(mFaceGraphic);
  }


  private void updatePreviousProportions(Face face) {
    for (Landmark landmark : face.getLandmarks()) {
      PointF position = landmark.getPosition();
      float xProp = (position.x - face.getPosition().x) / face.getWidth();
      float yProp = (position.y - face.getPosition().y) / face.getHeight();
      mPreviousProportions.put(landmark.getType(), new PointF(xProp, yProp));
    }
  }

  // Given a face and a facial landmark position,
  // return the coordinates of the landmark if known,
  // or approximated coordinates (based on prior data) if not.
  private PointF getLandmarkPosition(Face face, int landmarkId) {
    for (Landmark landmark : face.getLandmarks()) {
      if (landmark.getType() == landmarkId) {
        return landmark.getPosition();
      }
    }

    PointF prop = mPreviousProportions.get(landmarkId);
    if (prop == null) {
      return null;
    }

    float x = face.getPosition().x + (prop.x * face.getWidth());
    float y = face.getPosition().y + (prop.y * face.getHeight());
    return new PointF(x, y);
  }
}
