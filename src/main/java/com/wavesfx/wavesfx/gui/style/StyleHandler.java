package com.wavesfx.wavesfx.gui.style;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.scene.control.Control;

public class StyleHandler {
    public static final String ALERT_STYLE = "alert";

    public static void setConditionedStyle(boolean bool, Control control, String style){
        if (!bool) {
            if (!control.getStyleClass().contains(style))
                control.getStyleClass().add(style);
        } else {
            control.getStyleClass().removeAll(style);
        }
    }

    public static void setBorder(boolean bool, Control control) {
        setConditionedStyle(bool, control, ALERT_STYLE);
    }

    public static Disposable setBorderDisposable(Observable<Boolean> observable, Control control){
        return observable.observeOn(JavaFxScheduler.platform())
                .subscribe(b -> setBorder(b, control));
    }
}
