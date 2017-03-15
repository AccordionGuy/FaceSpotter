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

// See https://developers.google.com/android/reference/com/google/android/gms/vision/Tracker

class FaceTracker extends Tracker<Face> {

  private static final String TAG = "FaceTracker";

  // "Threshold" constants
  private static final float EYE_CLOSED_THRESHOLD = 0.4f;
  private static final float SMILING_THRESHOLD = 0.8f;

  private GraphicOverlay mOverlay;
  private FaceGraphic mFaceGraphic;
  private Context mContext;
  private boolean mIsFrontFacing;
  private FaceData mFaceData;

  // Subjects may move too quickly to for the system to detect their detect features,
  // or they may move so their features are out of the tracker's detection range.
  // This map keeps track of previously detected facial landmarks so that we can approximate
  // their locations when they momentarily "disappear".
  private Map<Integer, PointF> mPreviousLandmarkPositions = new HashMap<>();

  // As with facial landmarks, we keep track of the eye’s previous open/closed states
  // so that we can use them during those moments when they momentarily go undetected.
  private boolean mPreviousIsLeftOpen = true;
  private boolean mPreviousIsRightOpen = true;


  FaceTracker(GraphicOverlay overlay, Context context, boolean isFrontFacing) {
    mOverlay = overlay;
    mContext = context;
    mIsFrontFacing = isFrontFacing;
    mFaceData = new FaceData();
  }

  // Face detection event handlers
  // =============================

  // This method is called when a new face is detected.
  // We'll create a new graphic overlay whenever this happens.
  @Override
  public void onNewItem(int id, Face face) {
    mFaceGraphic = new FaceGraphic(mOverlay, mContext, mIsFrontFacing);
  }

  // As detected faces are tracked over time, this method is called regularly to update their information.
  // We'll collect the updated face information and use it to update the graphic overlay.
  @Override
  public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
    mOverlay.add(mFaceGraphic);
    updatePreviousLandmarkPositions(face);

    // Get face dimensions.
    mFaceData.setPosition(face.getPosition());
    mFaceData.setWidth(face.getWidth());
    mFaceData.setHeight(face.getHeight());

    // Get head angles.
    mFaceData.setEulerY(face.getEulerY());
    mFaceData.setEulerZ(face.getEulerZ());

    // Get the positions of facial landmarks.
    mFaceData.setLeftEyePosition(getLandmarkPosition(face, Landmark.LEFT_EYE));
    mFaceData.setRightEyePosition(getLandmarkPosition(face, Landmark.RIGHT_EYE));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_CHEEK));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_CHEEK));
    mFaceData.setNoseBasePosition(getLandmarkPosition(face, Landmark.NOSE_BASE));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.LEFT_EAR_TIP));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.RIGHT_EAR_TIP));
    mFaceData.setMouthLeftPosition(getLandmarkPosition(face, Landmark.LEFT_MOUTH));
    mFaceData.setMouthBottomPosition(getLandmarkPosition(face, Landmark.BOTTOM_MOUTH));
    mFaceData.setMouthRightPosition(getLandmarkPosition(face, Landmark.RIGHT_MOUTH));

    // Determine if eyes are open.
    float leftOpenScore = face.getIsLeftEyeOpenProbability();
    if (leftOpenScore == Face.UNCOMPUTED_PROBABILITY) {
      mFaceData.setLeftEyeOpen(mPreviousIsLeftOpen);
    } else {
      mFaceData.setLeftEyeOpen(leftOpenScore > EYE_CLOSED_THRESHOLD);
      mPreviousIsLeftOpen = mFaceData.isLeftEyeOpen();
    }
    float rightOpenScore = face.getIsRightEyeOpenProbability();
    if (rightOpenScore == Face.UNCOMPUTED_PROBABILITY) {
      mFaceData.setRightEyeOpen(mPreviousIsRightOpen);
    } else {
      mFaceData.setRightEyeOpen(rightOpenScore > EYE_CLOSED_THRESHOLD);
      mPreviousIsRightOpen = mFaceData.isRightEyeOpen();
    }

    // Determine if person is smiling.
    mFaceData.setSmiling(face.getIsSmilingProbability() > SMILING_THRESHOLD);

    // Update the graphic overlay.
    mFaceGraphic.update(mFaceData);
  }

  // This method is called when a face momentarily goes undetected.
  @Override
  public void onMissing(FaceDetector.Detections<Face> detectionResults) {
    mOverlay.remove(mFaceGraphic);
  }

  // This method is called when a face is assumed to be out of camera view for good.
  @Override
  public void onDone() {
    mOverlay.remove(mFaceGraphic);
  }

  // Facial landmark utility methods
  // ===============================

  // Given a face and a facial landmark position,
  // return the coordinates of the landmark if known,
  // or approximated coordinates (based on prior data) if not.
  private PointF getLandmarkPosition(Face face, int landmarkId) {
    for (Landmark landmark : face.getLandmarks()) {
      if (landmark.getType() == landmarkId) {
        return landmark.getPosition();
      }
    }

    PointF landmarkPosition = mPreviousLandmarkPositions.get(landmarkId);
    if (landmarkPosition == null) {
      return null;
    }

    float x = face.getPosition().x + (landmarkPosition.x * face.getWidth());
    float y = face.getPosition().y + (landmarkPosition.y * face.getHeight());
    return new PointF(x, y);
  }

  private void updatePreviousLandmarkPositions(Face face) {
    for (Landmark landmark : face.getLandmarks()) {
      PointF position = landmark.getPosition();
      float xProp = (position.x - face.getPosition().x) / face.getWidth();
      float yProp = (position.y - face.getPosition().y) / face.getHeight();
      mPreviousLandmarkPositions.put(landmark.getType(), new PointF(xProp, yProp));
    }
  }

}