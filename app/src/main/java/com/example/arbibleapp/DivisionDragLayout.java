package com.example.arbibleapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.widget.LinearLayout;

/** Observes coordinates before child dispatch so edge scrolling also works over drop slots. */
public class DivisionDragLayout extends LinearLayout {
    interface Observer { void onDrag(DragEvent event); }
    Observer observer;
    public DivisionDragLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override public boolean dispatchDragEvent(DragEvent event) {
        if (observer != null) observer.onDrag(event);
        return super.dispatchDragEvent(event);
    }
}
