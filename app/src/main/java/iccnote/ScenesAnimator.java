package iccnote;

import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.tajos.iccnotes.R;

public class ScenesAnimator {

    private final int mStartSceneId;
    private final int mEndSceneId;
    private final ViewGroup mSceneRoot;
    private Scene startScene, endScene;

    private Transition transition;

    private final AppCompatActivity mActivity;

    public ScenesAnimator(final AppCompatActivity activity, final ViewGroup sceneRoot, final int startSceneId, final int endSceneId) {
        mActivity = activity;
        mStartSceneId = startSceneId; mEndSceneId = endSceneId;
        mSceneRoot = sceneRoot;
        _initScenes();
    }

    private void _initScenes() {
        transition = TransitionInflater.from(mActivity).inflateTransition(R.transition.transition_set);
        //transition.addListener(listener);
        startScene = Scene.getSceneForLayout(mSceneRoot, mStartSceneId, mActivity);
        endScene = Scene.getSceneForLayout(mSceneRoot, mEndSceneId, mActivity);
    }

    public void animateToEndScene() {
        TransitionManager.go(endScene, transition);
    }

    public void animateToStartScene() {
        TransitionManager.go(startScene, transition);
    }

    protected Transition getTransition() {
        return transition;
    }
}
