package org.digitalcampus.oppia.widgets.quiz;


import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import ngo.lal.oppia.R;
import org.digitalcampus.mobile.quiz.Quiz;
import org.digitalcampus.mobile.quiz.model.QuizQuestion;
import org.digitalcampus.mobile.quiz.model.Response;

import java.util.ArrayList;
import java.util.List;

public class DragAndDropWidget extends QuestionWidget implements ViewTreeObserver.OnGlobalLayoutListener {

    private ViewGroup draggablesContainer, dropsContainer;

    private List<Dropzone> dropzones = new ArrayList<>();
    private List<Draggable> draggables = new ArrayList<>();
    private String courseLocation;

    private int backgroundWidth = 0, maxDragWidth = 0, maxDragHeight = 0;

    public DragAndDropWidget(Activity activity, View v, ViewGroup container, QuizQuestion q, String courseLocation) {
        super(activity, v, container, R.layout.widget_quiz_dragandrop);
        String dropzoneBackground = q.getProp("bgimage");
        this.courseLocation = courseLocation;

        String fileUrl = courseLocation + dropzoneBackground;
        Bitmap background = BitmapFactory.decodeFile(fileUrl);
        backgroundWidth = background.getWidth();
        ImageView dropzone = (ImageView) view.findViewById(R.id.dropzone_bg);
        dropzone.setImageBitmap(background);

        draggablesContainer = (ViewGroup) view.findViewById(R.id.drags_container);
        dropsContainer = (ViewGroup) view.findViewById(R.id.drops_container) ;

        // set up an observer that will be called once the layout is ready, to position the elements
        android.view.ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }

        draggablesContainer.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        v.setBackgroundResource(R.drawable.dragscontainer_normal);
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setBackgroundResource(R.drawable.dragscontainer_hover);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setBackgroundResource(R.drawable.dragscontainer_normal);
                        return true;
                    case DragEvent.ACTION_DROP:
                        // Dropped, reassign View to ViewGroup
                        View view = (View) event.getLocalState();
                        ViewGroup owner = (ViewGroup) view.getParent();
                        owner.removeView(view);
                        ViewGroup container = (ViewGroup) v;
                        container.addView(view);
                        view.setVisibility(View.VISIBLE);
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        v.setBackgroundResource(R.drawable.dragscontainer_normal);
                        View v2 = (View) event.getLocalState();
                        v2.setVisibility(View.VISIBLE);
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });

    }
    @Override
    public void setQuestionResponses(List<Response> responses, List<String> currentAnswers) {

        for (Response r : responses){

            String dropzone = r.getProp("dropzone");
            Dropzone drop = new Dropzone(ctx, dropzone);

            String xLeft= r.getProp("xleft");
            String yTop = r.getProp("ytop");
            if (xLeft != null && yTop != null){
                drop.setPosition(Integer.parseInt(xLeft), Integer.parseInt(yTop));
                drop.setOnDropListener(new OnDropListener() {
                    @Override
                    public void elemDropped(Draggable previousElem, Draggable newElem) {
                        if (previousElem != null){
                            draggablesContainer.addView(previousElem);
                        }
                    }
                });
                dropzones.add(drop);
                dropsContainer.addView(drop);
            }

            Draggable drag = new Draggable(ctx, dropzone);
            String dragImage = r.getProp("dragimage");
            if (dragImage != null){
                dragImage = courseLocation + dragImage;
                Bitmap dragBg = BitmapFactory.decodeFile(dragImage);
                maxDragWidth = Math.max(maxDragWidth, dragBg.getWidth());
                maxDragHeight = Math.max(maxDragHeight, dragBg.getHeight());
                drag.setImageBitmap(dragBg);
            }

            draggables.add(drag);
        }


        for (Draggable drag : draggables){
            boolean added = false;
            for (String answer : currentAnswers){
                String[] temp = answer.split(Quiz.MATCHING_REGEX,-1);
                if (temp.length < 2) continue;
                String dropzone = temp[0].trim();
                String draggable = temp[1].trim();

                if (drag.getDropzone().equals(draggable)){
                    for (Dropzone drop : dropzones){
                        if (drop.getDropZoneId().equals(dropzone)){
                            drop.addView(drag);
                            added = true;
                        }
                    }
                    break;
                }
            }

            if (!added){
                draggablesContainer.addView(drag);
            }
        }
        recalculateView();

    }

    @Override
    public List<String> getQuestionResponses(List<Response> responses) {

        List<String> userResponses = new ArrayList<>();
        for (Dropzone drop : dropzones){
            String response = drop.getResponse();
            if (response != null)
                userResponses.add(response);
        }
        return (userResponses.size() > 0 ? userResponses : null);
    }

    @Override
    public void onGlobalLayout() {
        recalculateView();
    }

    private void recalculateView(){
        int viewWidth = view.getMeasuredWidth();
        float ratio = (float) viewWidth / (float) backgroundWidth;

        for (Draggable drag : draggables){
            ViewGroup.LayoutParams params = drag.getLayoutParams();
            params.height = (int)(maxDragHeight * ratio);
            params.width = (int)(maxDragWidth * ratio);
        }

        for (Dropzone drop : dropzones){
            drop.repositionOnParent(ratio, maxDragHeight, maxDragWidth);
        }
    }

    interface OnDropListener{
        void elemDropped(Draggable previousElem, Draggable newElem);
    }


    class Draggable extends ImageView {

        private String dropzone;

        public Draggable(Context context) {
            super(context);
            this.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        public Draggable(Context context, String dropZone){
            this(context);
            this.dropzone = dropZone;
        }

        public String getDropzone() {
            return dropzone;
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent){
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                ClipData data = ClipData.newPlainText("", "");
                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(this);
                this.startDrag(data, shadowBuilder, this, 0);
                this.setVisibility(View.INVISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }



    class Dropzone extends FrameLayout {

        private static final int activeState = R.drawable.dropzone_active;
        private static final int hoverState = R.drawable.dropzone_hover;

        private String dropZoneId;
        private int topY;
        private int leftX;
        private OnDropListener onDropListener;

        public Dropzone(Context context, String dropzone) {
            super(context);
            this.dropZoneId = dropzone;
        }

        public void setPosition(int startX, int startY) {
            this.leftX = startX;
            this.topY = startY;
        }

        public void setOnDropListener(OnDropListener listener){
            onDropListener = listener;
        }

        public String getDropZoneId() {
            return dropZoneId;
        }

        public String getResponse() {
            Draggable current = getCurrentDraggable();
            if (current != null){
                return dropZoneId + Quiz.MATCHING_SEPARATOR + current.getDropzone();
            }
            else{
                return null;
            }

        }

        public void repositionOnParent(float ratio, int height, int width){
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
            params.height = (int)(height * ratio);
            params.width = (int)(width * ratio);
            params.leftMargin = (int)(leftX * ratio);
            params.topMargin = (int)(topY * ratio);
        }

        private Draggable getCurrentDraggable(){
            if (this.getChildCount() > 0){
                return (Draggable) getChildAt(0);
            }
            return null;
        }

        @Override
        public boolean onDragEvent(DragEvent event) {

            Draggable draggable = (Draggable) event.getLocalState();

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    setBackgroundResource(activeState);
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    setBackgroundResource(hoverState);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    setBackgroundResource(activeState);
                    return true;

                case DragEvent.ACTION_DROP:
                    // Dropped, reassign View to ViewGroup
                    ViewGroup owner = (ViewGroup) draggable.getParent();
                    owner.removeView(draggable);

                    Draggable previous = getCurrentDraggable();
                    this.removeAllViews();
                    this.addView(draggable);
                    draggable.setVisibility(View.VISIBLE);
                    onDropListener.elemDropped(previous, draggable);
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    setBackgroundResource(0);
                    return true;
                default:
                    break;
            }
            return false;
        }


    }

}
