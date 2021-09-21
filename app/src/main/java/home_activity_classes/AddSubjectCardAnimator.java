package home_activity_classes;

import android.transition.Transition;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import iccnote.ScenesAnimator;

public class AddSubjectCardAnimator extends ScenesAnimator {

    private Transition.TransitionListener transitionListener;

    private final ViewGroup mSceneRoot;

    private OnAnimationFinishedListener mListener;
    public interface OnAnimationFinishedListener {
        void isDropped();
    }

    public AddSubjectCardAnimator(AppCompatActivity activity, ViewGroup sceneRoot, int startSceneId, int endSceneId) {
        super(activity, sceneRoot, startSceneId, endSceneId);
        this.mSceneRoot = sceneRoot;
        _initListener();

        getTransition().addListener(transitionListener);
    }

    private void _initListener() {
        transitionListener = new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                if (isToDropped) {
                    mSceneRoot.getLayoutParams().height = mSceneRoot.getMeasuredHeight();
                    mSceneRoot.requestLayout();
                    mListener.isDropped();
                }

                if (! isToDropped) {
                    mSceneRoot.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    mSceneRoot.requestLayout();
                    mListener.isDropped();
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        };
    }

    private boolean isToDropped = false;
    public void drop() {
        isToDropped = true;
        animateToEndScene();
        //TransitionManager.go(endScene, transition);
    }

    public void hide() {
        isToDropped = false;
        animateToStartScene();
        //TransitionManager.go(startScene, transition);
    }

    public void setOnAnimationFinishedListener(final OnAnimationFinishedListener listener) {
        mListener = listener;
    }
}
